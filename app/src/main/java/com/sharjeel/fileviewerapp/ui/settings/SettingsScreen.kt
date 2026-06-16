package com.sharjeel.fileviewerapp.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharjeel.fileviewerapp.ui.theme.GlassSurface
import com.sharjeel.fileviewerapp.ui.theme.NeonPrimary
import com.sharjeel.fileviewerapp.ui.theme.NeonSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // FIXED TOP BAR (Outside the scrollable Column)
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "SETTINGS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()) // Respect TopBar height
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // PROFILE CARD (Premium Dashboard Style like HomeScreen)
            ProfileDashboardCard()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SettingsSectionHeader("GENERAL")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) GlassSurface else MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column {
                    SettingsToggleItem(
                        title = "Dark Theme",
                        icon = Icons.Rounded.DarkMode,
                        iconColor = Color(0xFFBB86FC),
                        checked = isDark,
                        onCheckedChange = { /* TODO */ }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    SettingsClickItem(
                        title = "Language",
                        icon = Icons.Rounded.Translate,
                        iconColor = NeonSecondary,
                        onClick = { /* TODO */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            SettingsSectionHeader("PRIVACY")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) GlassSurface else MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column {
                    SettingsToggleItem(
                        title = "Biometric Lock",
                        icon = Icons.Rounded.Fingerprint,
                        iconColor = Color(0xFF69F0AE),
                        checked = true,
                        onCheckedChange = { /* TODO */ }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    SettingsClickItem(
                        title = "Vault PIN",
                        icon = Icons.Rounded.LockOpen,
                        iconColor = Color(0xFFFFD740),
                        onClick = { /* TODO */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            SettingsSectionHeader("SYSTEM")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) GlassSurface else MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column {
                    SettingsClickItem(
                        title = "Clear Cache",
                        icon = Icons.Rounded.CleaningServices,
                        iconColor = Color(0xFFFF5252),
                        onClick = { /* TODO */ }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    SettingsToggleItem(
                        title = "Hidden Files",
                        icon = Icons.Rounded.Visibility,
                        iconColor = NeonPrimary,
                        checked = false,
                        onCheckedChange = { /* TODO */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Version 1.0.2 Premium",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 40.dp))
        }
    }
}

@Composable
fun ProfileDashboardCard() {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) GlassSurface else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(NeonPrimary.copy(alpha = 0.15f), Color.Transparent)
                            } else {
                                listOf(NeonPrimary.copy(alpha = 0.05f), Color.Transparent)
                            }
                        )
                    )
            )
            
            Row(
                modifier = Modifier.padding(24.dp).fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = NeonPrimary.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPrimary.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = NeonPrimary, modifier = Modifier.size(32.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column {
                    Text("Sharjeel Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Pro Account Active", style = MaterialTheme.typography.labelMedium, color = NeonSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = NeonSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
    )
}

@Composable
fun SettingsClickItem(
    title: String,
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
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(18.dp))
        
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
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
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(18.dp))
        
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
}
