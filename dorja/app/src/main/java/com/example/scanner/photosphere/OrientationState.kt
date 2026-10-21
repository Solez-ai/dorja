package com.example.scanner.photosphere

import android.content.Context
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun rememberOrientationTracker(
    samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME,
    reference: OrientationReference = OrientationReference.Camera,
): OrientationTracker {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tracker = remember(context, samplingPeriodUs, reference) {
        OrientationTracker(context, samplingPeriodUs, reference)
    }

    val configuration = LocalConfiguration.current
    val displayRotation = remember(context, configuration) { context.currentDisplayRotation() }
    SideEffect { tracker.displayRotation = displayRotation }

    DisposableEffect(lifecycleOwner, tracker) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> tracker.startListening()
                Lifecycle.Event.ON_STOP -> tracker.stopListening()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        tracker.startListening()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            tracker.stopListening()
        }
    }

    return tracker
}

@Composable
fun rememberOrientationData(
    samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME,
    reference: OrientationReference = OrientationReference.Camera,
): State<OrientationData> =
    rememberOrientationTracker(samplingPeriodUs, reference)
        .orientation
        .collectAsState()

internal fun Context.currentDisplayRotation(): Int = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.rotation
    } else {
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation
    }
}.getOrNull() ?: Surface.ROTATION_0
