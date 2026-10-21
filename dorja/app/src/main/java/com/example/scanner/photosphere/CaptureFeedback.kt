package com.example.scanner.photosphere

import android.content.Context
import android.media.MediaActionSound
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val TAG = "CaptureFeedback"
private const val FALLBACK_TICK_MILLIS = 20L

class CaptureFeedback(context: Context) {

    private val vibrator: Vibrator? = context.vibrator()

    private val shutterSound = MediaActionSound().apply {
        runCatching { load(MediaActionSound.SHUTTER_CLICK) }
    }

    private var isReleased = false

    fun onFrameCaptured() {
        if (isReleased) return
        playTick()
        runCatching { shutterSound.play(MediaActionSound.SHUTTER_CLICK) }
            .onFailure { Log.w(TAG, "Shutter sound failed", it) }
    }

    fun playDwellTick() {
        if (isReleased) return
        playTick()
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        runCatching { shutterSound.release() }
    }

    private fun playTick() {
        val vibrator = vibrator ?: return
        if (!vibrator.hasVibrator()) return

        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            FALLBACK_TICK_MILLIS,
                            VibrationEffect.DEFAULT_AMPLITUDE,
                        )
                    )
                }
                else -> {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(FALLBACK_TICK_MILLIS)
                }
            }
        }.onFailure { Log.w(TAG, "Haptic tick failed", it) }
    }
}

@Composable
fun rememberCaptureFeedback(): CaptureFeedback {
    val context = LocalContext.current
    val feedback = remember(context) { CaptureFeedback(context) }
    DisposableEffect(feedback) {
        onDispose { feedback.release() }
    }
    return feedback
}

private fun Context.vibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
