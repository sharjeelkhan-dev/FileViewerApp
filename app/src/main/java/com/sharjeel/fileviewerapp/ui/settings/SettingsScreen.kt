package com.sharjeel.fileviewerapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharjeel.fileviewerapp.ui.components.AppScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    AppScaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Header with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .offset(x = (-10).dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "SETTINGS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // GENERAL Category
            SettingsSectionHeader(title = "GENERAL", headerColor = Color(0xFF03DAC6))
            SettingsContainer {
                SettingsToggleItem(
                    title = "Dark Theme",
                    subtitle = "Always stay on the dark side",
                    icon = Icons.Rounded.DarkMode,
                    iconColor = Color(0xFFBB86FC),
                    checked = isSystemInDarkTheme(),
                    onCheckedChange = { /* TODO */ }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
                SettingsClickItem(
                    title = "App Language",
                    subtitle = "English (United States)",
                    icon = Icons.Rounded.Language,
                    iconColor = Color(0xFF03DAC6),
                    onClick = { /* TODO */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SECURITY Category
            SettingsSectionHeader(title = "SECURITY", headerColor = Color(0xFF03DAC6))
            SettingsContainer {
                SettingsToggleItem(
                    title = "Biometric Lock",
                    subtitle = "Fingerprint or Face ID",
                    icon = Icons.Rounded.Fingerprint,
                    iconColor = Color(0xFF00E676),
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
                SettingsClickItem(
                    title = "Safe Vault PIN",
                    subtitle = "Protect your private files",
                    icon = Icons.Rounded.VpnKey,
                    iconColor = Color(0xFFFFC400),
                    onClick = { /* TODO */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ADVANCED Category
            SettingsSectionHeader(title = "ADVANCED", headerColor = Color(0xFF03DAC6))
            SettingsContainer {
                SettingsClickItem(
                    title = "Clear Cache",
                    subtitle = "Free up memory instantly",
                    icon = Icons.Rounded.CleaningServices,
                    iconColor = Color(0xFFFF1744),
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
                SettingsToggleItem(
                    title = "Hidden Files",
                    subtitle = "Show system .nomedia files",
                    icon = Icons.Rounded.Visibility,
                    iconColor = Color(0xFF6200EE),
                    checked = false,
                    onCheckedChange = { /* TODO */ }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun SettingsContainer(content: @Composable ColumnScope.() -> Unit) {
    val isDark = isSystemInDarkTheme()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        // Fallback for GlassSurface using direct semi-transparent dark shade
        color = if (isDark) Color(0xFF1E1E2C).copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsSectionHeader(title: String, headerColor: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = headerColor,
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
    )
}

@Composable
fun SettingsClickItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = iconColor.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = iconColor.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6200EE), // Fallback primary track
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        )
    }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
fun SettingsScreenDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(onBackClick = {})
        }
    }
}

@Preview(name = "Light Theme", showBackground = true)
@Composable
fun SettingsScreenLightPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(onBackClick = {})
        }
    }
}