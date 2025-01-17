package com.github.nacabaro.vbhelper.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.github.nacabaro.vbhelper.domain.Character
import com.github.nacabaro.vbhelper.domain.Sprites
import com.github.nacabaro.vbhelper.dtos.CharacterDtos

@Dao
interface CharacterDao {
    @Insert
    suspend fun insertCharacter(vararg characterData: Character)

    @Query("SELECT * FROM Character")
    suspend fun getAllCharacters(): List<Character>

    @Query("SELECT * FROM Character WHERE dimId = :dimId")
    suspend fun getCharacterByDimId(dimId: Int): List<Character>

    @Query("SELECT * FROM Character WHERE monIndex = :monIndex AND dimId = :dimId LIMIT 1")
    fun getCharacterByMonIndex(monIndex: Int, dimId: Long): Character

    @Insert
    suspend fun insertSprite(vararg sprite: Sprites)

    @Query("SELECT * FROM Sprites")
    suspend fun getAllSprites(): List<Sprites>

    @Query("""
        SELECT 
            d.dimId as cardId,
            c.monIndex as charId
        FROM Character c
        JOIN UserCharacter uc ON c.id = uc.charId
        JOIN Dim d ON c.dimId = d.id
        WHERE uc.id = :charId
    """)
    suspend fun getCharacterInfo(charId: Long): CharacterDtos.DiMInfo
}