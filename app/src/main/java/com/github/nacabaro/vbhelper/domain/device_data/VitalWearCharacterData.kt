package com.github.nacabaro.vbhelper.domain.device_data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.github.cfogrady.vbnfc.data.NfcCharacter
import com.github.cfogrady.vitalwear.protos.Character
import com.github.cfogrady.vitalwear.protos.Character.Settings.AllowedBattles
import com.google.protobuf.ByteString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream

const val APP_NAME = "VBHelper"

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = UserCharacter::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VitalWearCharacterData (
    @PrimaryKey(autoGenerate = true) val id: Long,
    val trainingHp: Int,
    val trainingAp: Int,
    val trainingBp: Int,
    var remainingTrainingTimeInMinutes: Int,
    // VitalWear specific values to make transferring back and forth easy.
    val trainingInBackground: Boolean,
    val allowedBattles: VitalWearAllowedBattles,
    val assumedFranchise: Int?,
    val maxAdventuresCompletedByCard: ByteArray,
    // App specific values
    val abilityRarity: NfcCharacter.AbilityRarity,
    val abilityType: Int,
    val abilityBranch: Int,
    val abilityReset: Int,
    val rank: Int,
    // data for other apps the character has been on
    val otherAppData: ByteArray,
) {

    fun maxAdventuresCompletedByCardAsMap(): Map<String, Int> {
        val resultMap = HashMap<String, Int>()
        val input = DataInputStream(ByteArrayInputStream(this.maxAdventuresCompletedByCard))
        while(input.available() > 0) {
            val cardName = input.readUTF()
            val maxCompletion = input.readInt()
            resultMap.put(cardName, maxCompletion)
        }
        return resultMap
    }

    private fun getAppDataAsBytes(): ByteArray {
        val vbHelperBytes = ByteArrayOutputStream()
        val vbHelperOutputStream = DataOutputStream(vbHelperBytes)
        vbHelperOutputStream.writeInt(1) // version
        vbHelperOutputStream.writeInt(this.abilityRarity.ordinal)
        vbHelperOutputStream.writeInt(this.abilityType)
        vbHelperOutputStream.writeInt(this.abilityBranch)
        vbHelperOutputStream.writeInt(this.abilityReset)
        vbHelperOutputStream.writeInt(this.rank)
        return vbHelperBytes.toByteArray()
    }

    fun getAppDataAsMap(): Map<String, ByteString> {
        val input = DataInputStream(ByteArrayInputStream(otherAppData))
        val appDataByName = HashMap<String, ByteString>()
        while(input.available() > 0) {
            val appName = input.readUTF()
            val dataSize = input.readInt()
            val appData = input.readNumBytes(dataSize)
            appDataByName.put(appName, ByteString.copyFrom(appData))
        }
        appDataByName.put(APP_NAME, ByteString.copyFrom(getAppDataAsBytes()))
        return appDataByName
    }

    companion object {

        fun buildVitalWearCharacterDataFromCharacterProto(character: Character): VitalWearCharacterData {
            val appData = character.getParsedVBHelperAppData()
            return VitalWearCharacterData(
                id = 0, // This auto-generated upon insert.
                trainingHp =  character.characterStats.trainedHp,
                trainingAp = character.characterStats.trainedAp,
                trainingBp = character.characterStats.trainedBp,
                remainingTrainingTimeInMinutes = (character.characterStats.trainingTimeRemainingInSeconds / 60).toInt(),
                trainingInBackground = character.settings.trainingInBackground,
                allowedBattles = VitalWearAllowedBattles.fromProtoEnum(character.settings.allowedBattles),
                assumedFranchise = character.getNullableAssumedFranchise(),
                maxAdventuresCompletedByCard = character.maxAdventuresCompletedAsByteArray(),
                abilityRarity = appData.abilityRarity,
                abilityType = appData.abilityType,
                abilityBranch = appData.abilityBranch,
                abilityReset = appData.abilityReset,
                rank = appData.rank,
                otherAppData = character.getOtherAppData()
            )
        }
    }
}

fun Character.maxAdventuresCompletedAsByteArray(): ByteArray {
    val byteStream = ByteArrayOutputStream()
    val output = DataOutputStream(byteStream)
    for(key in this.maxAdventureCompletedByCardMap.keys) {
        output.writeUTF(key)
        output.writeInt(this.maxAdventureCompletedByCardMap[key]!!)
    }
    return byteStream.toByteArray()
}

fun Character.getNullableAssumedFranchise(): Int? {
    if(this.settings.hasAssumedFranchise()) {
        return this.settings.assumedFranchise
    }
    return null
}

fun Character.getParsedVBHelperAppData(): VBHelperAppData {
    val bytes = this.appSpecificDataMap[APP_NAME]
    if(bytes == null || bytes.isEmpty) {
        return VBHelperAppData(NfcCharacter.AbilityRarity.None, 0, 0, 0, 0)
    }
    val dataStream = DataInputStream(bytes.newInput())
    val version = dataStream.readInt()
    if(version > 1) {
        throw IllegalArgumentException("Unsupported App Version!")
    }
    return VBHelperAppData(
        abilityRarity = NfcCharacter.AbilityRarity.entries[dataStream.readInt()],
        abilityType = dataStream.readInt(),
        abilityBranch = dataStream.readInt(),
        abilityReset = dataStream.readInt(),
        rank = dataStream.readInt())
}

fun Character.getOtherAppData(): ByteArray {
    val bytesStream = ByteArrayOutputStream()
    val dataStream = DataOutputStream(bytesStream)
    for(app in this.appSpecificDataMap) {
        if(APP_NAME.equals(app.key)) {
            continue
        }
        dataStream.writeUTF(app.key)
        val appBytes = app.value.toByteArray()
        dataStream.writeInt(appBytes.size)
        dataStream.write(appBytes)
    }
    return bytesStream.toByteArray()
}

data class VBHelperAppData(
    val abilityRarity: NfcCharacter.AbilityRarity,
    val abilityType: Int,
    val abilityBranch: Int,
    val abilityReset: Int,
    val rank: Int,
)

enum class VitalWearAllowedBattles {
    CardOnly,
    AllFranchise,
    AllFranchiseAndDim,
    All;

    companion object {
        fun fromProtoEnum(protoEnum: AllowedBattles): VitalWearAllowedBattles {
            return when(protoEnum) {
                AllowedBattles.ALL -> All
                AllowedBattles.CARD_ONLY -> CardOnly
                AllowedBattles.ALL_FRANCHISE -> AllFranchise
                AllowedBattles.ALL_FRANCHISE_AND_DIM -> AllFranchiseAndDim
                AllowedBattles.UNRECOGNIZED -> {
                    throw IllegalArgumentException("Unrecognized Allow Battles Setting")
                }
            }
        }
    }
}

fun InputStream.readNumBytes(n: Int): ByteArray {
    val bytes = ByteArray(n)
    var readIndex = 0
    while(readIndex < n) {
        val readBytes = this.read(bytes, readIndex, n-readIndex)
        if(readBytes < 0) {
            return bytes.sliceArray(0 until readIndex)
        }
        readIndex += readBytes
    }
    return bytes
}

