package com.github.nacabaro.vbhelper.domain.device_data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
    var version: Int,
)