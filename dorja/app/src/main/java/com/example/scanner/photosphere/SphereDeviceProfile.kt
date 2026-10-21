package com.example.scanner.photosphere

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class SphereDeviceProfile(
    val preferWidestCamera: Boolean,
    val stitchFrameMaxEdgePx: Int,
    val stitchCanvasWidthPx: Int,
    val stitchCanvasHeightPx: Int,
) {
    companion object {
        private const val LOW_RAM_THRESHOLD_BYTES = 4L * 1024L * 1024L * 1024L

        fun forDevice(context: Context? = null): SphereDeviceProfile {
            val isLowRam = context?.let { ctx ->
                val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val info = ActivityManager.MemoryInfo()
                am?.getMemoryInfo(info)
                am?.isLowRamDevice == true || (info.totalMem in 1 until LOW_RAM_THRESHOLD_BYTES)
            } ?: false

            // Prefer the main camera (preferWidestCamera = false) unless on a device where
            // ultrawide is explicitly requested, because the main camera has cleaner edges,
            // higher sharpness, and consistent intrinsics across Android phones.
            return if (isLowRam) {
                SphereDeviceProfile(
                    preferWidestCamera = false,
                    stitchFrameMaxEdgePx = 1280,
                    stitchCanvasWidthPx = 3072,
                    stitchCanvasHeightPx = 1536,
                )
            } else {
                SphereDeviceProfile(
                    preferWidestCamera = false,
                    stitchFrameMaxEdgePx = 1600,
                    stitchCanvasWidthPx = 4096,
                    stitchCanvasHeightPx = 2048,
                )
            }
        }

        val isSamsung: Boolean
            get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }
}
