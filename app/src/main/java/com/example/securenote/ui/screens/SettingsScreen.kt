package com.example.securenote.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.securenote.SecureNoteApp
import com.example.securenote.util.Clipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SecureNoteApp
    val km = app.container.vaultKeyManager

    var biometricEnabled by remember { mutableStateOf(km.isBiometricEnabled) }
    var autoClearEnabled by remember { mutableStateOf(Clipboard.isAutoClearEnabled(ctx)) }
    val canAuth = remember {
        BiometricManager.from(ctx).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Security", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Biometric unlock")
                    Text(
                        text = when {
                            !canAuth -> "Not available on this device"
                            biometricEnabled -> "Enabled — you can unlock with fingerprint or face"
                            else -> "Off — unlock with master password only"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = biometricEnabled,
                    enabled = canAuth,
                    onCheckedChange = { checked ->
                        if (checked) {
                            enrollBiometric(ctx as FragmentActivity) { ok ->
                                biometricEnabled = ok
                                Toast.makeText(
                                    ctx,
                                    if (ok) "Biometric unlock enabled" else "Enrollment cancelled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            km.disableBiometric()
                            biometricEnabled = false
                            Toast.makeText(ctx, "Biometric unlock disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            Text("Clipboard", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Auto-clear password after 20s")
                    Text(
                        text = if (autoClearEnabled)
                            "Copied passwords are removed from the clipboard 20 seconds later"
                        else
                            "Passwords stay in the clipboard until you replace them",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoClearEnabled,
                    onCheckedChange = { checked ->
                        autoClearEnabled = checked
                        Clipboard.setAutoClearEnabled(ctx, checked)
                    }
                )
            }

            Text(
                text = "Losing your master password means losing your notes — there is no recovery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun enrollBiometric(activity: FragmentActivity, onDone: (Boolean) -> Unit) {
    val app = activity.applicationContext as SecureNoteApp
    val km = app.container.vaultKeyManager
    val session = app.container.session

    val rawKey = session.copyRawKey()
    if (rawKey == null) {
        Toast.makeText(activity, "Vault is locked; unlock first", Toast.LENGTH_SHORT).show()
        onDone(false); return
    }

    val cipher = try {
        km.cipherForBiometricEnroll()
    } catch (t: Throwable) {
        rawKey.fill(0)
        Toast.makeText(activity, "Keystore error: ${t.message}", Toast.LENGTH_LONG).show()
        onDone(false); return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            val authorized = result.cryptoObject?.cipher
            if (authorized == null) {
                rawKey.fill(0); onDone(false); return
            }
            try {
                km.enrollBiometric(rawKey, authorized)
                onDone(true)
            } catch (t: Throwable) {
                Toast.makeText(activity, "Enroll failed: ${t.message}", Toast.LENGTH_LONG).show()
                onDone(false)
            } finally {
                rawKey.fill(0)
            }
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            rawKey.fill(0); onDone(false)
        }
        override fun onAuthenticationFailed() { /* retry allowed */ }
    })

    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Enable biometric unlock")
        .setSubtitle("Authenticate to bind SecureNote to this device's biometrics")
        .setNegativeButtonText("Cancel")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()
    prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
}
