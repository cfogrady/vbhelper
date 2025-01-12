package com.github.nacabaro.vbhelper.source

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.github.nacabaro.vbhelper.source.proto.VitalWearSettings
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

private const val VITAL_WEAR_DATA_STORE_NAME = "vital_wear_settings.pb"

val Context.vitalWearSettingsDataStore: DataStore<VitalWearSettings> by dataStore(
    fileName = VITAL_WEAR_DATA_STORE_NAME,
    serializer = VitalWearSettingsSerializer
)

object VitalWearSettingsSerializer: Serializer<VitalWearSettings> {
    override val defaultValue = VitalWearSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): VitalWearSettings {
        try {
            return VitalWearSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: VitalWearSettings, output: OutputStream) {
        t.writeTo(output)
    }
}