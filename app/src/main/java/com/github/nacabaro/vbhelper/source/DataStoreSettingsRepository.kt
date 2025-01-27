package com.github.nacabaro.vbhelper.source

import androidx.datastore.core.DataStore
import com.github.nacabaro.vbhelper.source.proto.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single

class DataStoreSettingsRepository(
    private val settingsDataStore: DataStore<Settings>
) : SettingsRepository {
    override val settingsFlow: Flow<Settings> = settingsDataStore.data
    override suspend fun getSettings(): Settings {
        return settingsFlow.single()
    }

    override suspend fun enableVitalWearOptions(enabled: Boolean) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setVitalWearEnabled(enabled)
                .build()
        }
    }
}