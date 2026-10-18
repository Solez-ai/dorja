package com.example.scanner

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

object HapticFeedback {

    fun confirmTap(context: Context) {
        vibrate(context, longArrayOf(0, 50, 30, 50), -1)
    }

    fun errorBuzz(context: Context) {
        vibrate(context, longArrayOf(0, 100, 50, 100), -1)
    }

    fun guidanceVibration(context: Context) {
        vibrate(context, longArrayOf(0, 40, 20, 40, 20, 60), -1)
    }

    private fun vibrate(context: Context, pattern: LongArray, amplitude: Int) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService<VibratorManager>()
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.cancel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, amplitude)
                vibrator?.vibrate(effect)
            }
        } catch (e: Exception) {
            // Silently fail if vibration unavailable
        }
    }
}
