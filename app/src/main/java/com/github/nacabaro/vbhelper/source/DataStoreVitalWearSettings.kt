package com.github.nacabaro.vbhelper.source

import androidx.datastore.core.DataStore
import com.github.nacabaro.vbhelper.source.proto.Secrets

class DataStoreVitalWearSettings(
    private val vitalWearDataStore: DataStore<Secrets>,
) {
}