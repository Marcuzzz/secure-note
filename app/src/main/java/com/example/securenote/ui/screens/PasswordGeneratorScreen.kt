package com.example.securenote.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.securenote.util.PasswordGenerator
import com.example.securenote.util.PasswordOptions
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(onBack: () -> Unit) {
    var length by remember { mutableStateOf(20f) }
    var upper by remember { mutableStateOf(true) }
    var lower by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var avoidAmbiguous by remember { mutableStateOf(false) }

    val options = PasswordOptions(
        length = length.roundToInt(),
        includeUppercase = upper,
        includeLowercase = lower,
        includeDigits = digits,
        includeSymbols = symbols,
        avoidAmbiguous = avoidAmbiguous,
    )
    var password by remember { mutableStateOf(PasswordGenerator.generate(options)) }
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password generator") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = password.ifBlank { "(no character set selected)" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { password = PasswordGenerator.generate(options) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Text("  Regenerate")
                        }
                        Button(
                            onClick = {
                                if (password.isNotBlank()) copy(ctx, password)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null)
                            Text("  Copy")
                        }
                    }
                }
            }

            StrengthMeter(bits = PasswordGenerator.estimateStrengthBits(options))

            Text("Length: ${length.roundToInt()}", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = length,
                onValueChange = { length = it; password = PasswordGenerator.generate(options.copy(length = it.roundToInt())) },
                valueRange = 6f..64f,
                steps = 57
            )

            ToggleRow("Uppercase (A-Z)", upper) { upper = it; password = PasswordGenerator.generate(options.copy(includeUppercase = it)) }
            ToggleRow("Lowercase (a-z)", lower) { lower = it; password = PasswordGenerator.generate(options.copy(includeLowercase = it)) }
            ToggleRow("Digits (0-9)", digits) { digits = it; password = PasswordGenerator.generate(options.copy(includeDigits = it)) }
            ToggleRow("Symbols (!@#…)", symbols) { symbols = it; password = PasswordGenerator.generate(options.copy(includeSymbols = it)) }
            ToggleRow("Avoid ambiguous (O/0, l/1)", avoidAmbiguous) { avoidAmbiguous = it; password = PasswordGenerator.generate(options.copy(avoidAmbiguous = it)) }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StrengthMeter(bits: Double) {
    val fraction = (bits / 128.0).toFloat().coerceIn(0f, 1f)
    val label = when {
        bits < 40 -> "Weak"
        bits < 60 -> "Fair"
        bits < 80 -> "Strong"
        else -> "Very strong"
    }
    Column {
        Text("Strength: $label (${bits.roundToInt()} bits)", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
    }
}

private fun copy(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("password", text)
    cm.setPrimaryClip(clip)
    Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
}
