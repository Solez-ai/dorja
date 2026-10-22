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
            val am = context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val info = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(info)
            val isLowRam = am?.isLowRamDevice == true ||
                (info.totalMem in 1 until LOW_RAM_THRESHOLD_BYTES)
            val maxHeapMb = (Runtime.getRuntime().maxMemory() / (1024L * 1024L)).toInt()

            // Match 360-photo-app's memory-safe profile:
            // - A 1280px upright portrait frame (~960x1280) over a ~54° horizontal FOV has
            //   960 / 54 ≈ 17.8 pixels per degree -> 17.8 * 360 ≈ 6400 px/turn, which more than
            //   saturates a 4096×2048 equirectangular sphere without wasting heap.
            // - On constrained heaps (<300 MB max heap or low-RAM device), use 1024px frame edge
            //   (matching 360-photo-app's DEFAULT_MAX_INPUT_DIMENSION = 1024).
            return if (isLowRam || maxHeapMb < 300) {
                SphereDeviceProfile(
                    preferWidestCamera = false,
                    stitchFrameMaxEdgePx = 1024,
                    stitchCanvasWidthPx = 4096,
                    stitchCanvasHeightPx = 2048,
                )
            } else {
                SphereDeviceProfile(
                    preferWidestCamera = false,
                    stitchFrameMaxEdgePx = 1280,
                    stitchCanvasWidthPx = 4096,
                    stitchCanvasHeightPx = 2048,
                )
            }
        }

        val isSamsung: Boolean
            get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }
}
