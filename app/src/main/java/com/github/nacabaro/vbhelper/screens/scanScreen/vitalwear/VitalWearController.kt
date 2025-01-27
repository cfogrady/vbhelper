package com.github.nacabaro.vbhelper.screens.scanScreen.vitalwear

import com.github.cfogrady.vitalwear.protos.Character
import com.github.cfogrady.vitalwear.transfer.CharacterTransfer

interface VitalWearController {

    companion object {
        val MOCK_VITAL_WEAR_CONTROLLER = object: VitalWearController {
            override suspend fun checkVitalWearPermissionsAndRequestForMissing(): Boolean {
                return true
            }

            override fun createCharacterTransfer(): CharacterTransfer {
                return CharacterTransfer()
            }

            override fun getActiveCharacter(): Character {
                TODO("Not yet implemented")
            }

            override suspend fun receiveCharacter(character: Character): Boolean {
                TODO("Not yet implemented")
            }

        }
    }

    suspend fun checkVitalWearPermissionsAndRequestForMissing(): Boolean

    fun createCharacterTransfer(): CharacterTransfer

    fun getActiveCharacter(): Character

    suspend fun receiveCharacter(character: Character): Boolean
}