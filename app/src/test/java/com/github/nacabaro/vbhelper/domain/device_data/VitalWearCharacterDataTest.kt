package com.github.nacabaro.vbhelper.domain.device_data

import com.github.cfogrady.vbnfc.data.NfcCharacter
import com.github.cfogrady.vitalwear.protos.Character
import com.google.protobuf.ByteString
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class VitalWearCharacterDataTest {
    @Test
    fun testThatAdventureCompletionMapIsSerializedCorrectly() {
        val expectedMap = mapOf(
            Pair("TestCard1", 4),
            Pair("AnotherCard4", 1),
            Pair("FinalCard", 12)
        )
        val character = Character.newBuilder()
            .putAllMaxAdventureCompletedByCard(expectedMap)
            .setCharacterStats(Character.CharacterStats.newBuilder().build())
            .setSettings(Character.Settings.newBuilder().build())
            .build()
        val vitalWearCharacterData =
            VitalWearCharacterData.buildVitalWearCharacterDataFromCharacterProto(character)
        val mapFromCharacterData = vitalWearCharacterData.maxAdventuresCompletedByCardAsMap()
        Assert.assertEquals(expectedMap, mapFromCharacterData)
    }

    fun vbHelperAppDataAsByteArray(abilityRarity: NfcCharacter.AbilityRarity, abilityType: Int, abilityBranch: Int, abilityReset: Int, rank: Int): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val dataStream = DataOutputStream(byteStream)
        dataStream.writeInt(1) // version
        dataStream.writeInt(abilityRarity.ordinal)
        dataStream.writeInt(abilityType)
        dataStream.writeInt(abilityBranch)
        dataStream.writeInt(abilityReset)
        dataStream.writeInt(rank)
        return byteStream.toByteArray()
    }

    @Test
    fun testThatAppDataIsSerializedCorrectly() {
        val expectedAppData = mapOf(
            Pair(APP_NAME, ByteString.copyFrom(vbHelperAppDataAsByteArray(NfcCharacter.AbilityRarity.SuperRare, 1, 2, 3, 4))),
            Pair("other_app", ByteString.copyFrom(byteArrayOf(0, 0x49, 0x50, 0x60, 0x79, 0))),
            Pair("final_app", ByteString.copyFrom(byteArrayOf(0, 1, 2, 3, 4, 5, 7, 7, 8, 9))),
        )
        val character = Character.newBuilder()
            .putAllAppSpecificData(expectedAppData)
            .setCharacterStats(Character.CharacterStats.newBuilder().build())
            .setSettings(Character.Settings.newBuilder().build())
            .build()
        val vitalWearCharacterData =
            VitalWearCharacterData.buildVitalWearCharacterDataFromCharacterProto(character)
        val appDataAsMap = vitalWearCharacterData.getAppDataAsMap()
        Assert.assertEquals(expectedAppData, appDataAsMap)
    }

    @Test
    fun testThatAppDataIsAddedAsBlankIfMissing() {
        val expectedAppData = mapOf(
            Pair("other_app", ByteString.copyFrom(byteArrayOf(0, 0x49, 0x50, 0x60, 0x79, 0))),
            Pair("final_app", ByteString.copyFrom(byteArrayOf(0, 1, 2, 3, 4, 5, 7, 7, 8, 9))),
        )
        val character = Character.newBuilder()
            .putAllAppSpecificData(expectedAppData)
            .setCharacterStats(Character.CharacterStats.newBuilder().build())
            .setSettings(Character.Settings.newBuilder().build())
            .build()
        val vitalWearCharacterData =
            VitalWearCharacterData.buildVitalWearCharacterDataFromCharacterProto(character)
        val appDataAsMap = vitalWearCharacterData.getAppDataAsMap()
        Assert.assertEquals(expectedAppData["other_app"], appDataAsMap["other_app"])
        Assert.assertEquals(expectedAppData["final_app"], appDataAsMap["final_app"])
        Assert.assertTrue(appDataAsMap.containsKey(APP_NAME))
        Assert.assertEquals(NfcCharacter.AbilityRarity.None, vitalWearCharacterData.abilityRarity)
        Assert.assertEquals(0, vitalWearCharacterData.abilityType)
        Assert.assertEquals(0, vitalWearCharacterData.abilityBranch)
        Assert.assertEquals(0, vitalWearCharacterData.abilityReset)
        Assert.assertEquals(0, vitalWearCharacterData.rank)
    }
}