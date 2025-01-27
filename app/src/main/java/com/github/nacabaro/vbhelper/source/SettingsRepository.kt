package com.github.nacabaro.vbhelper.source

import com.github.nacabaro.vbhelper.source.proto.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<Settings>

    suspend fun getSettings(): Settings
    suspend fun enableVitalWearOptions(enabled: Boolean)
}