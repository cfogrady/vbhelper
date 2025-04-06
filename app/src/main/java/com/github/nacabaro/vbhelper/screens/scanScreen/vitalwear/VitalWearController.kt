package com.github.nacabaro.vbhelper.screens.scanScreen.vitalwear

import com.github.cfogrady.vitalwear.protos.Character
import com.github.cfogrady.vitalwear.transfer.CharacterTransfer
import com.github.nacabaro.vbhelper.domain.characters.Card
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

interface VitalWearController {

    companion object {
        val MOCK_VITAL_WEAR_CONTROLLER = object: VitalWearController {
            override suspend fun checkVitalWearPermissionsAndRequestForMissing(): Boolean {
                return true
            }
            override fun createCharacterTransfer(): CharacterTransfer {
                return object: CharacterTransfer {
                    override val deviceName = "TEST"
                    override fun close() {}
                    override fun receiveCharacterFrom(
                        senderName: String,
                        receive: suspend (Character) -> Boolean
                    ): StateFlow<CharacterTransfer.Result> {
                        return MutableStateFlow(CharacterTransfer.Result.TRANSFERRING)
                    }
                    override fun searchForOtherTransferDevices(): Flow<String> {
                        return flow {"ABCD"}
                    }
                    override fun sendCharacterToDevice(
                        senderName: String,
                        character: Character
                    ): StateFlow<CharacterTransfer.Result> {
                        return MutableStateFlow(CharacterTransfer.Result.TRANSFERRING)
                    }
                }
            }
            override fun getActiveCharacter(): Character {
                return Character.getDefaultInstance()
            }
            override suspend fun receiveCharacter(card: Card, character: Character): Boolean {
                return true
            }
            override fun deleteCharacter() {}
            override fun toast(message: String) {}

        }
    }

    suspend fun checkVitalWearPermissionsAndRequestForMissing(): Boolean

    fun createCharacterTransfer(): CharacterTransfer

    fun getActiveCharacter(): Character

    suspend fun receiveCharacter(card: Card, character: Character): Boolean

    fun deleteCharacter()

    fun toast(message: String)
}