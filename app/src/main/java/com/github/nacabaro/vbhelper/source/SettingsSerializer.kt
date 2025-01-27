package com.github.nacabaro.vbhelper.source

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.github.nacabaro.vbhelper.source.proto.Settings
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

private const val SETTINGS_DATA_STORE_NAME = "settings.pb"

val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = SETTINGS_DATA_STORE_NAME,
    serializer = SettingsSerializer
)

object SettingsSerializer: Serializer<Settings> {
    override val defaultValue = Settings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Settings {
        try {
            return Settings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        t.writeTo(output)
    }

}