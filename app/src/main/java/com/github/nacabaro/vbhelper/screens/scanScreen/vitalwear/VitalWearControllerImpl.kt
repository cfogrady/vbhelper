package com.github.nacabaro.vbhelper.screens.scanScreen.vitalwear

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.util.fastJoinToString
import com.github.cfogrady.vbnfc.be.BENfcCharacter
import com.github.cfogrady.vitalwear.protos.Character
import com.github.cfogrady.vitalwear.transfer.CharacterTransfer
import com.github.nacabaro.vbhelper.di.VBHelper
import com.github.nacabaro.vbhelper.domain.characters.Card
import com.github.nacabaro.vbhelper.domain.device_data.BECharacterData
import com.github.nacabaro.vbhelper.domain.device_data.UserCharacter
import com.github.nacabaro.vbhelper.domain.device_data.VitalWearCharacterData
import com.github.nacabaro.vbhelper.utils.DeviceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.GregorianCalendar

class VitalWearControllerImpl(private val activity: ComponentActivity): VitalWearController {

    private val missingPermissionsLauncherForVitalWearConnection: ActivityResultLauncher<Array<String>>
    private val addPermissionsSuccessResult = MutableSharedFlow<Boolean>()

    init {
        missingPermissionsLauncherForVitalWearConnection = buildPermissionRequestLauncher { requestedPermissions->
            val deniedPermissions = mutableListOf<String>()
            for(requestedPermission in requestedPermissions) {
                if(!requestedPermission.value) {
                    deniedPermissions.add(requestedPermission.key)
                }
            }
            if(deniedPermissions.isNotEmpty()) {
                toast("Permission Required For VitalWear Transfers")
            }
            CoroutineScope(Dispatchers.Default).launch {
                addPermissionsSuccessResult.emit(deniedPermissions.isEmpty())
            }
        }
    }

    override suspend fun checkVitalWearPermissionsAndRequestForMissing(): Boolean {
        val missingPermissions = CharacterTransfer.getMissingPermissions(activity)
        Log.i("VitalWearControllerImpl", "Missing Permissions: ${missingPermissions.fastJoinToString(",")}")
        if(missingPermissions.isEmpty()) {
            return true
        }
        val job = CoroutineScope(Dispatchers.Default).async {
            addPermissionsSuccessResult.first()
        }
        missingPermissionsLauncherForVitalWearConnection.launch(missingPermissions.toTypedArray())
        return job.await()
    }

    private fun buildPermissionRequestLauncher(resultBehavior: (Map<String, Boolean>)->Unit): ActivityResultLauncher<Array<String>> {
        val multiplePermissionsContract = ActivityResultContracts.RequestMultiplePermissions()
        val launcher = activity.registerForActivityResult(multiplePermissionsContract, resultBehavior)
        return launcher
    }

    override fun createCharacterTransfer(): CharacterTransfer {
        return CharacterTransfer.getInstance(activity)
    }

    override fun getActiveCharacter(): Character {
        return tmpCharacter
    }

    var tmpCharacter = Character.getDefaultInstance()

    suspend fun findCard(cardName: String): Card? {
        val application = activity.applicationContext as VBHelper
        val storageRepository = application.container.db

        return storageRepository
            .dimDao()
            .getDimByName(cardName)
    }

    override suspend fun receiveCharacter(card: Card, character: Character): Boolean {
        try {
            val application = activity.applicationContext as VBHelper
            val storageRepository = application.container.db

            val cardCharData = storageRepository
                .characterDao()
                .getCharacterByMonIndex(character.characterStats.slotId, card.id)

            val characterData = buildUserCharacter(cardCharData.id, character)

            storageRepository
                .userCharacterDao()
                .clearActiveCharacter()

            val characterId: Long = storageRepository
                .userCharacterDao()
                .insertCharacterData(characterData)

            val extraCharacterData = buildVitalWearCharacterData(characterId, character)

            storageRepository
                .userCharacterDao()
                .insertVitalWearCharacterData(extraCharacterData)

            val transformationHistoryWatch = character.transformationHistoryList
            transformationHistoryWatch.map { item ->
                val date = LocalDateTime.of(0, 0, 0, 0, 0, 0).toEpochSecond(
                    OffsetDateTime.now().offset)

                storageRepository
                    .characterDao()
                    .insertTransformation(characterId, item.slotId, card.id, date)

                storageRepository
                    .dexDao()
                    .insertCharacter(item.slotId, card.id, date)
            }

            return true
        } catch (iae: IllegalArgumentException) {
            toast("Transfer Cancelled: ${iae.message}")
            return false
        }
    }

    private fun buildUserCharacter(characterId: Long, character: Character): UserCharacter {
        return UserCharacter(
            charId = characterId,
            ageInDays = nfcCharacter.ageInDays.toInt(),
            nextAdventureMissionStage = nfcCharacter.nextAdventureMissionStage.toInt(),
            mood = nfcCharacter.mood.toInt(),
            vitalPoints = nfcCharacter.vitalPoints.toInt(),
            transformationCountdown = nfcCharacter.transformationCountdownInMinutes.toInt(),
            injuryStatus = nfcCharacter.injuryStatus,
            trophies = nfcCharacter.trophies.toInt(),
            currentPhaseBattlesWon = nfcCharacter.currentPhaseBattlesWon.toInt(),
            currentPhaseBattlesLost = nfcCharacter.currentPhaseBattlesLost.toInt(),
            totalBattlesWon = nfcCharacter.totalBattlesWon.toInt(),
            totalBattlesLost = nfcCharacter.totalBattlesLost.toInt(),
            activityLevel = nfcCharacter.activityLevel.toInt(),
            heartRateCurrent = nfcCharacter.heartRateCurrent.toInt(),
            characterType = when (nfcCharacter) {
                is BENfcCharacter -> DeviceType.BEDevice
                else -> DeviceType.VBDevice
            },
            isActive = true
        )
    }

    private fun buildVitalWearCharacterData(vbHelperCharacterId: Long, character: Character): VitalWearCharacterData {
        return VitalWearCharacterData(
            id = vbHelperCharacterId,
            trainingHp = nfcCharacter.trainingHp.toInt(),
            trainingAp = nfcCharacter.trainingAp.toInt(),
            trainingBp = nfcCharacter.trainingBp.toInt(),
            remainingTrainingTimeInMinutes = nfcCharacter.remainingTrainingTimeInMinutes.toInt(),
            itemEffectActivityLevelValue = nfcCharacter.itemEffectActivityLevelValue.toInt(),
            itemEffectMentalStateValue = nfcCharacter.itemEffectMentalStateValue.toInt(),
            itemEffectMentalStateMinutesRemaining = nfcCharacter.itemEffectMentalStateMinutesRemaining.toInt(),
            itemEffectActivityLevelMinutesRemaining = nfcCharacter.itemEffectActivityLevelMinutesRemaining.toInt(),
            itemEffectVitalPointsChangeValue = nfcCharacter.itemEffectVitalPointsChangeValue.toInt(),
            itemEffectVitalPointsChangeMinutesRemaining = nfcCharacter.itemEffectVitalPointsChangeMinutesRemaining.toInt(),
            abilityRarity = nfcCharacter.abilityRarity,
            abilityType = nfcCharacter.abilityType.toInt(),
            abilityBranch = nfcCharacter.abilityBranch.toInt(),
            abilityReset = nfcCharacter.abilityReset.toInt(),
            rank = nfcCharacter.abilityReset.toInt(),
            itemType = nfcCharacter.itemType.toInt(),
            itemMultiplier = nfcCharacter.itemMultiplier.toInt(),
            itemRemainingTime = nfcCharacter.itemRemainingTime.toInt(),
            otp0 = "", //nfcCharacter.value!!.otp0.toString(),
            otp1 = "", //nfcCharacter.value!!.otp1.toString(),
            minorVersion = nfcCharacter.characterCreationFirmwareVersion.minorVersion.toInt(),
            majorVersion = nfcCharacter.characterCreationFirmwareVersion.majorVersion.toInt(),
        )
    }

    override fun deleteCharacter() {

    }

    override fun toast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }
}