package com.github.nacabaro.vbhelper.screens.scanScreen.vitalwear

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.cfogrady.nearby.connections.p2p.ui.DisplayMatchingDevices
import com.github.cfogrady.vitalwear.transfer.CharacterTransfer
import com.github.nacabaro.vbhelper.screens.scanScreen.ScanScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun VitalWearTransfer(vitalWearController: VitalWearController, scanScreenState: ScanScreenState, onComplete: (success: Boolean) -> Unit) {
    if(scanScreenState != ScanScreenState.VITALWEAR_TO_APP && scanScreenState != ScanScreenState.APP_TO_VITALWEAR) {
        throw IllegalArgumentException("VitalWearTransfer can only take VITALWEAR_TO_APP or APP_TO_VITALWEAR")
    }
    var state by remember { mutableStateOf(VitalWearTransferState.FIND_DEVICES) }
    val characterTransfer = remember { vitalWearController.createCharacterTransfer() }
    val coroutineScope = rememberCoroutineScope()

    var result = remember { MutableStateFlow(CharacterTransfer.Result.TRANSFERRING) }
    when(state) {
        VitalWearTransferState.FIND_DEVICES -> FindDevices(characterTransfer) { deviceName ->
            if(scanScreenState == ScanScreenState.APP_TO_VITALWEAR) {
                CoroutineScope(Dispatchers.IO).launch {
                    val character = vitalWearController.getActiveCharacter()
                    val transferResult = characterTransfer.sendCharacterToDevice(deviceName, character)
                    coroutineScope.launch {
                        transferResult.collect{ transferResultValue ->
                            result.update { transferResultValue }
                        }
                    }
                }
            } else {
                val transferResult = characterTransfer.receiveCharacterFrom(deviceName, vitalWearController::receiveCharacter)
                coroutineScope.launch {
                    transferResult.collect { transferResultValue ->
                        result.update { transferResultValue }
                    }
                }
            }
            state = VitalWearTransferState.CONNECTING
        }
        VitalWearTransferState.CONNECTING -> {
            val connectionStatus by result.collectAsState()
            if(connectionStatus == CharacterTransfer.Result.TRANSFERRING) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Transferring...")
                }
            } else {
                state = VitalWearTransferState.COMPLETE
            }
        }
        VitalWearTransferState.COMPLETE -> {
            TransferResult(vitalWearController, scanScreenState, result, onComplete)
        }
    }
}


@Composable
fun FindDevices(characterTransfer: CharacterTransfer, onDeviceFound: (String)->Unit) {
    val discoveredDevices = remember { MutableSharedFlow<String>() }
    var connected by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(true) {
        coroutineScope.launch {
            characterTransfer.searchForOtherTransferDevices().collect {
                discoveredDevices.emit(it)
            }
        }

        onDispose {
            if(!connected) {
                characterTransfer.close()
            }
        }
    }
    DisplayMatchingDevices(characterTransfer.deviceName, discoveredDevices, rescan = {
        connected = false
        characterTransfer.close()
        characterTransfer.searchForOtherTransferDevices()
    }, selectDevice = {
        connected = true
        onDeviceFound.invoke(it)
    })
}

@Composable
fun TransferResult(vitalWearController: VitalWearController, scanScreenState: ScanScreenState, resultStatusFlow: StateFlow<CharacterTransfer.Result>, onComplete: (success: Boolean)->Unit) {
    val resultStatus = remember { resultStatusFlow.value }
    when(resultStatus) {
        CharacterTransfer.Result.TRANSFERRING -> {
            throw IllegalStateException("Shouldn't be looking at result is the status is still Trasnferring")
        }
        CharacterTransfer.Result.SUCCESS -> {
            if(scanScreenState == ScanScreenState.APP_TO_VITALWEAR) {
                LaunchedEffect(true) {
                    vitalWearController.deleteCharacter()
                }
                vitalWearController.toast("Sent")
                onComplete.invoke(true)
                // SendAnimation(idleBitmap = idle, walkBitmap = walk) { finish() }
            } else {
                // ReceiveAnimation(receiveCharacterSprites!!.idle, receiveCharacterSprites!!.happy) { finish() }
                vitalWearController.toast("Transfer Received!")
                onComplete.invoke(true)
            }
        }
        CharacterTransfer.Result.REJECTED -> {
            vitalWearController.toast("Transfer Rejected!")
            onComplete.invoke(false)
        }
        CharacterTransfer.Result.FAILURE -> {
            vitalWearController.toast("Transfer Failed!")
            onComplete.invoke(false)
        }
    }
}

//@Composable
//fun SendAnimation(idleBitmap: Bitmap, walkBitmap: Bitmap, onComplete: ()->Unit) {
//    var targetAnimation by remember { mutableStateOf(0) }
//    var idle by remember { mutableStateOf(true) }
//    val flicker by animateIntAsState(targetAnimation, tween(
//        durationMillis = 3000,
//        easing = FastOutLinearInEasing
//    )
//    ) {
//        if(it == 11) {
//            onComplete.invoke()
//        }
//    }
//    LaunchedEffect(true) {
//        delay(500)
//        idle = false
//        delay(500)
//        targetAnimation = 11
//    }
//    vitalBoxFactory.VitalBox {
//        bitmapScaler.ScaledBitmap(transferBackground, "Background", alignment = Alignment.BottomCenter)
//
//        if(flicker % 2 == 0) {
//            bitmapScaler.ScaledBitmap(if(idle) idleBitmap else walkBitmap, "Character", alignment = Alignment.BottomCenter,
//                modifier = Modifier.offset(y = backgroundHeight.times(-0.05f)))
//        }
//    }
//}
//
//@Composable
//fun ReceiveAnimation(idleBitmap: Bitmap, happyBitmap: Bitmap, onComplete: () -> Unit) {
//    var targetAnimation by remember { mutableStateOf(0) }
//    var idle by remember { mutableStateOf(false) }
//    var startIdleFlip by remember { mutableStateOf(false) }
//    val flicker by animateIntAsState(targetAnimation, tween(
//        durationMillis = 3000,
//        easing = LinearOutSlowInEasing
//    )
//    ) {
//        if(it == 11) {
//            startIdleFlip = true
//        }
//    }
//    LaunchedEffect(true) {
//        targetAnimation = 11
//    }
//    LaunchedEffect(startIdleFlip) {
//        if(startIdleFlip) {
//            idle = true
//            delay(500)
//            idle = false
//            delay(500)
//            idle = true
//            delay(500)
//            idle = false
//            onComplete.invoke()
//
//        }
//    }
//    vitalBoxFactory.VitalBox {
//        bitmapScaler.ScaledBitmap(transferBackground, "Background", alignment = Alignment.BottomCenter)
//
//        if(flicker % 2 == 1) {
//            bitmapScaler.ScaledBitmap(if(idle) idleBitmap else happyBitmap, "Character", alignment = Alignment.BottomCenter,
//                modifier = Modifier.offset(y = backgroundHeight.times(-0.05f)))
//        }
//    }
//}
