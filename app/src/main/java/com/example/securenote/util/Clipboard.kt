package com.example.securenote.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Clipboard {

    private const val PREFS = "app_prefs"
    private const val KEY_AUTO_CLEAR = "clipboard_auto_clear"
    const val CLEAR_DELAY_MS = 20_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pendingClear: Job? = null

    fun isAutoClearEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_CLEAR, true)

    fun setAutoClearEnabled(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_CLEAR, enabled).apply()
    }

    fun copyPlain(ctx: Context, label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
    }

    /** Copies a password. Marks it sensitive on Android 13+ and schedules auto-clear if enabled. */
    fun copyPassword(ctx: Context, password: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("password", password)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        cm.setPrimaryClip(clip)

        val autoClear = isAutoClearEnabled(ctx)
        val msg = if (autoClear) "Copied — clears in ${CLEAR_DELAY_MS / 1000}s" else "Copied"
        // On Android 13+ the system already shows a "Copied" chip; skip the toast so it isn't doubled.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }

        if (autoClear) scheduleClear(ctx.applicationContext, password)
    }

    private fun scheduleClear(appCtx: Context, sentinel: String) {
        pendingClear?.cancel()
        pendingClear = scope.launch {
            delay(CLEAR_DELAY_MS)
            val cm = appCtx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val current = cm.primaryClip?.getItemAt(0)?.text?.toString()
            if (current == sentinel) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cm.clearPrimaryClip()
                } else {
                    cm.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        }
    }
}
