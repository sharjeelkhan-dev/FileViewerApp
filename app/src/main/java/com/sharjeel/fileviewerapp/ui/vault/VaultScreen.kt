package com.sharjeel.fileviewerapp.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import com.sharjeel.fileviewerapp.util.BiometricHelper

@Composable
fun VaultScreen(
    onAccessGranted: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Secure Vault", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("Authentication required to access", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                activity?.let {
                    BiometricHelper.showBiometricPrompt(
                        activity = it,
                        title = "Vault Access",
                        subtitle = "Authenticate to open the vault",
                        onSuccess = onAccessGranted,
                        onError = { error -> errorMessage = error }
                    )
                }
            }) {
                Text("Unlock with Biometrics")
            }
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Preview(showBackground = true, name = "Vault Light Mode")
@Composable
fun VaultPreviewLight() {
    FileViewerAppTheme(darkTheme = false) {
        VaultScreen(onAccessGranted = {})
    }
}

@Preview(showBackground = true, name = "Vault Dark Mode")
@Composable
fun VaultPreviewDark() {
    FileViewerAppTheme(darkTheme = true) {
        VaultScreen(onAccessGranted = {})
    }
}
