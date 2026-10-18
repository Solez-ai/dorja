package com.example.scanner

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.util.Range
import androidx.lifecycle.LifecycleOwner
import androidx.camera.view.PreviewView
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
class CameraManagerTest {
    private lateinit var context: Context
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var previewView: PreviewView
    private lateinit var mockAndroidCameraManager: AndroidCameraManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        lifecycleOwner = mockk(relaxed = true)
        previewView = mockk(relaxed = true)
        mockAndroidCameraManager = mockk(relaxed = true)

        every { context.getSystemService(Context.CAMERA_SERVICE) } returns
            mockAndroidCameraManager
    }

    /**
     * Property 2: Zoom Slider Constraints
     * Validates: Requirements 4.1, 4.2
     *
     * For any device with detected zoom capabilities (minZoom, maxZoom),
     * the default zoom SHALL be within the detected range.
     */
    @Test
    fun `detected default zoom is within min and max range`() = runTest {
        val cameraManager =
            CameraManager(context, lifecycleOwner, previewView)

        // Mock a back-facing camera with zoom range
        every { mockAndroidCameraManager.cameraIdList } returns
            arrayOf("0")

        val characteristics = mockk<CameraCharacteristics>()
        every {
            mockAndroidCameraManager.getCameraCharacteristics("0")
        } returns characteristics

        // Mock zoom range [0.5, 4.0]
        every {
            characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        } returns Range(0.5f, 4.0f)

        // Mock lens facing (back camera)
        every {
            characteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_BACK

        // Mock focal lengths (standard lens)
        every {
            characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        } returns floatArrayOf(28f)

        val capabilities = cameraManager.detectZoomCapabilities()

        assertTrue(
            capabilities.defaultZoom >= capabilities.minZoomRatio &&
                capabilities.defaultZoom <= capabilities.maxZoomRatio,
            "Default zoom must be within detected range"
        )
    }

    /**
     * Tests that ultra-wide lens is correctly detected from focal length < 24mm
     */
    @Test
    fun `ultra wide lens detected when focal length less than 24mm`() = runTest {
        val cameraManager =
            CameraManager(context, lifecycleOwner, previewView)

        every { mockAndroidCameraManager.cameraIdList } returns
            arrayOf("0")

        val characteristics = mockk<CameraCharacteristics>()
        every {
            mockAndroidCameraManager.getCameraCharacteristics("0")
        } returns characteristics

        every {
            characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        } returns Range(0.5f, 2.0f)

        every {
            characteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_BACK

        // Ultra-wide focal length
        every {
            characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        } returns floatArrayOf(13f)

        val capabilities = cameraManager.detectZoomCapabilities()

        assertTrue(
            capabilities.hasUltraWide,
            "Ultra-wide lens should be detected for focal length 13mm"
        )
        assertEquals(
            0.8f,
            capabilities.defaultZoom,
            "Default zoom should be 0.8x for ultra-wide lens"
        )
    }

    /**
     * Tests that standard lens is correctly detected from focal length 24-35mm
     */
    @Test
    fun `standard lens detected when focal length between 24 and 35mm`() = runTest {
        val cameraManager =
            CameraManager(context, lifecycleOwner, previewView)

        every { mockAndroidCameraManager.cameraIdList } returns
            arrayOf("0")

        val characteristics = mockk<CameraCharacteristics>()
        every {
            mockAndroidCameraManager.getCameraCharacteristics("0")
        } returns characteristics

        every {
            characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        } returns Range(1.0f, 2.0f)

        every {
            characteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_BACK

        // Standard focal length
        every {
            characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        } returns floatArrayOf(28f)

        val capabilities = cameraManager.detectZoomCapabilities()

        assertTrue(
            capabilities.hasStandard,
            "Standard lens should be detected for focal length 28mm"
        )
        assertEquals(
            1.0f,
            capabilities.defaultZoom,
            "Default zoom should be 1.0x for standard lens"
        )
    }

    /**
     * Tests that telephoto lens is correctly detected from focal length > 50mm
     */
    @Test
    fun `telephoto lens detected when focal length greater than 50mm`() = runTest {
        val cameraManager =
            CameraManager(context, lifecycleOwner, previewView)

        every { mockAndroidCameraManager.cameraIdList } returns
            arrayOf("0")

        val characteristics = mockk<CameraCharacteristics>()
        every {
            mockAndroidCameraManager.getCameraCharacteristics("0")
        } returns characteristics

        every {
            characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        } returns Range(1.0f, 3.0f)

        every {
            characteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_BACK

        // Telephoto focal length
        every {
            characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        } returns floatArrayOf(75f)

        val capabilities = cameraManager.detectZoomCapabilities()

        assertTrue(
            capabilities.hasTelephoto,
            "Telephoto lens should be detected for focal length 75mm"
        )
    }

    /**
     * Tests that safe defaults are used when zoom metadata is unavailable
     */
    @Test
    fun `safe defaults applied when zoom metadata unavailable`() = runTest {
        val cameraManager =
            CameraManager(context, lifecycleOwner, previewView)

        every { mockAndroidCameraManager.cameraIdList } returns
            arrayOf("0")

        val characteristics = mockk<CameraCharacteristics>()
        every {
            mockAndroidCameraManager.getCameraCharacteristics("0")
        } returns characteristics

        // No zoom range available
        every {
            characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        } returns null

        every {
            characteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_BACK

        every {
            characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        } returns null

        val capabilities = cameraManager.detectZoomCapabilities()

        assertEquals(
            1.0f,
            capabilities.minZoomRatio,
            "Min zoom should be default 1.0x"
        )
        assertEquals(
            2.0f,
            capabilities.maxZoomRatio,
            "Max zoom should be default 2.0x"
        )
    }

    /**
     * Tests that error recovery works and returns safe defaults on exception
     */
    @Test
    fun `error recovery returns safe defaults on exception`() = runTest {
        val cameraManager =
            CameraManager(context, lifecycleOwner, previewView)

        every { mockAndroidCameraManager.cameraIdList } throws
            RuntimeException("Camera hardware error")

        val capabilities = cameraManager.detectZoomCapabilities()

        assertTrue(capabilities.isValid(), "Safe defaults should be valid")
        assertEquals(1.0f, capabilities.minZoomRatio)
        assertEquals(2.0f, capabilities.maxZoomRatio)
    }

    /**
     * Tests that empty camera list returns safe defaults
     */
    @Test
    fun `safe defaults applied when no cameras found`() = runTest {
        val cameraManager =
            CameraManager(context, lifecycleOwner, previewView)

        every { mockAndroidCameraManager.cameraIdList } returns
            arrayOf()

        val capabilities = cameraManager.detectZoomCapabilities()

        assertTrue(capabilities.isValid(), "Safe defaults should be valid")
        assertEquals(1.0f, capabilities.minZoomRatio)
        assertEquals(2.0f, capabilities.maxZoomRatio)
    }

    /**
     * Tests that zoom constraints logic correctly constrains default zoom
     * within the detected range (min/max bounds checking)
     */
    @Test
    fun `zoom capabilities are valid and consistent`() = runTest {
        val cameraManager =
            CameraManager(context, lifecycleOwner, previewView)

        every { mockAndroidCameraManager.cameraIdList } returns
            arrayOf("0")

        val characteristics = mockk<CameraCharacteristics>()
        every {
            mockAndroidCameraManager.getCameraCharacteristics("0")
        } returns characteristics

        // Wide zoom range
        every {
            characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        } returns Range(0.5f, 8.0f)

        every {
            characteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_BACK

        every {
            characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        } returns floatArrayOf(28f)

        val capabilities = cameraManager.detectZoomCapabilities()

        assertTrue(
            capabilities.isValid(),
            "Detected capabilities should pass validation"
        )
        assertTrue(
            capabilities.defaultZoom >= capabilities.minZoomRatio &&
                capabilities.defaultZoom <= capabilities.maxZoomRatio,
            "Default zoom must be within min/max range"
        )
    }

    /**
     * Tests that front-facing cameras are skipped (only back-facing used for scanning)
     */
    @Test
    fun `front facing cameras skipped for scanning`() = runTest {
        val cameraManager =
            CameraManager(context, lifecycleOwner, previewView)

        // Two cameras: front and back
        every { mockAndroidCameraManager.cameraIdList } returns
            arrayOf("0", "1")

        val frontCharacteristics = mockk<CameraCharacteristics>()
        val backCharacteristics = mockk<CameraCharacteristics>()

        every {
            mockAndroidCameraManager.getCameraCharacteristics("0")
        } returns frontCharacteristics
        every {
            mockAndroidCameraManager.getCameraCharacteristics("1")
        } returns backCharacteristics

        // Front camera
        every {
            frontCharacteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_FRONT

        // Back camera with zoom
        every {
            backCharacteristics.get(CameraCharacteristics.LENS_FACING)
        } returns CameraCharacteristics.LENS_FACING_BACK

        every {
            backCharacteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        } returns Range(0.5f, 4.0f)

        every {
            backCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        } returns floatArrayOf(28f)

        val capabilities = cameraManager.detectZoomCapabilities()

        assertTrue(
            capabilities.minZoomRatio >= 0.5f,
            "Should use back camera zoom range, not front"
        )
    }
}
