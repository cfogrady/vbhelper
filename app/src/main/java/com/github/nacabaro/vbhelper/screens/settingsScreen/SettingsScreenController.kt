package com.github.nacabaro.vbhelper.screens.settingsScreen

import com.github.nacabaro.vbhelper.source.proto.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsScreenController {
    fun onClickOpenDirectory()
    fun onClickImportDatabase()
    fun onClickImportApk()
    fun onClickImportCard()
    fun setVitalWearEnabled(enabled: Boolean)
    val settings: Flow<Settings>
}