package com.github.nacabaro.vbhelper.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.github.nacabaro.vbhelper.components.TopBanner
import com.github.nacabaro.vbhelper.source.proto.VitalWearSettings

@Composable
fun SettingsScreen(
    navController: NavController,
    settingsScreenController: SettingsScreenController,
    onClickImportCard: () -> Unit
) {
    val vitalWearSettings by settingsScreenController.vitalWearSettings.collectAsState(VitalWearSettings.getDefaultInstance())
    val vitalWearEnableText = if(vitalWearSettings.enabled) "Disable" else "Enable"

    Scaffold (
        topBar = {
            TopBanner(
                text = "Settings",
                onBackClick = {
                    navController.popBackStack()
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
    ) { contentPadding ->
        Column (
            modifier = Modifier
                .padding(top = contentPadding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection("NFC Communication")
            SettingsEntry(title = "Import APK", description = "Import Secrets From Vital Arean 2.1.0 APK") {
                settingsScreenController.apkFilePickLauncher.launch(arrayOf("*/*"))
            }
            SettingsSection("DiM/BEm management")
            SettingsEntry(title = "Import DiM card", description = "Import DiM/BEm card file", onClick = onClickImportCard)
            SettingsEntry(title = "Rename DiM/BEm", description = "Set card name") { }
            SettingsSection("Other Devices")
            SettingsEntry(title = "$vitalWearEnableText VitalWear Settings", description = "${vitalWearEnableText}s settings for VitalWear") {
                val newSettings = vitalWearSettings.toBuilder().setEnabled(!vitalWearSettings.enabled).build()
                settingsScreenController.updateVitalWearSettings(newSettings)
            }
            SettingsSection("About and credits")
            SettingsEntry(title = "Credits", description = "Credits") { }
            SettingsEntry(title = "About", description = "About") { }
        }
    }
}

@Composable
fun SettingsEntry(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(text = title)
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun SettingsSection(
    title: String
) {
    Box (
        modifier = Modifier
            .padding(start = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}