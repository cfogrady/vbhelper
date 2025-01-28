package com.github.nacabaro.vbhelper.screens.scanScreen.vitalwear

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.util.fastJoinToString
import com.github.cfogrady.vitalwear.protos.Character
import com.github.cfogrady.vitalwear.transfer.CharacterTransfer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    override suspend fun receiveCharacter(character: Character): Boolean {
        tmpCharacter = character
        return true
    }

    override fun deleteCharacter() {

    }

    override fun toast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }
}