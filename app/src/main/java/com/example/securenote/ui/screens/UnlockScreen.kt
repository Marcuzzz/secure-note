package com.example.securenote.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.securenote.ui.vm.UnlockViewModel

@Composable
fun UnlockScreen(vm: UnlockViewModel) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
            vm.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null)
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (state.isInitialized) "Unlock SecureNote" else "Create your vault",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(24.dp))

        if (state.isInitialized) UnlockForm(vm, state)
        else CreateVaultForm(vm)

        if (state.busy) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun CreateVaultForm(vm: UnlockViewModel) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Master password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = confirm,
        onValueChange = { confirm = it },
        label = { Text("Confirm password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { vm.createVault(password.toCharArray(), confirm.toCharArray()); password = ""; confirm = "" },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Create vault") }
    Spacer(Modifier.height(8.dp))
    Text("This password encrypts your notes. It cannot be recovered if lost.", fontSize = 12.sp)
}

@Composable
private fun UnlockForm(vm: UnlockViewModel, state: com.example.securenote.ui.vm.UnlockUiState) {
    var password by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Master password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { vm.unlockWithPassword(password.toCharArray()); password = "" },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Unlock") }

    if (state.biometricAvailable) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { launchBiometricUnlock(ctx as FragmentActivity, vm) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Fingerprint, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text("  Use biometric")
        }
    }
    Spacer(Modifier.height(24.dp))
    TextButton(onClick = { /* no-op placeholder for future reset flow */ }) {
        Text("Forgot password?")
    }
}

private fun launchBiometricUnlock(activity: FragmentActivity, vm: UnlockViewModel) {
    val manager = BiometricManager.from(activity)
    val canAuth = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
        Toast.makeText(activity, "Biometric not available", Toast.LENGTH_SHORT).show()
        return
    }
    val app = activity.application as com.example.securenote.SecureNoteApp
    val cipher = app.container.vaultKeyManager.cipherForBiometricUnlock() ?: run {
        Toast.makeText(activity, "Biometric not enrolled", Toast.LENGTH_SHORT).show()
        return
    }
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            val authorized = result.cryptoObject?.cipher ?: return
            vm.unlockWithBiometric(authorized)
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            Toast.makeText(activity, errString, Toast.LENGTH_SHORT).show()
        }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock SecureNote")
        .setSubtitle("Authenticate to open your vault")
        .setNegativeButtonText("Cancel")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()
    prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
}
