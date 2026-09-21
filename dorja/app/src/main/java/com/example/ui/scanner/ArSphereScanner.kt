package com.example.ui.scanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.i18n.L
import com.example.ui.i18n.LocalDorjaLocale
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.LightEstimate
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════
//  AR SPHERE ENGINE — real ARCore session with pose, plane and
//  light estimation driving guided spherical capture.
// ═════════════════════════════════════════════════════════════

/** Live AR understanding of the room, posted every camera frame. */
internal data class ArSphereStatus(
    val active: Boolean,
    val headingDeg: Float,
    val pitchDeg: Float,
    val floorFound: Boolean,
    val ceilingFound: Boolean,
    val wallsFound: Int,
    val lowLight: Boolean,
    val failureMsg: String?
)

internal class ArSphereCaptureEngine {

    companion object {
        private const val TAG = "ArSphereScanner"

        fun checkAvailability(ctx: Context): ArCoreApk.Availability =
            try { ArCoreApk.getInstance().checkAvailability(ctx) } catch (_: Throwable) { ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE }

        /** Launches the Google Play Services for AR install flow if needed. */
        fun requestInstall(activity: Activity): Boolean = try {
            ArCoreApk.getInstance().requestInstall(activity, true)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "ARCore install failed", t)
            false
        }
    }

    var session: Session? = null
        private set

    private var glView: GLSurfaceView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    /** Latest known orientation + room understanding (posted on the main thread). */
    var onStatus: ((ArSphereStatus) -> Unit)? = null
    var lastHeading = 0f
        private set
    var lastPitch = 0f
        private set

    var displayRotation = android.view.Surface.ROTATION_0
        private set
    var captureRotationDeg = 90f
        private set

    internal var pendingCapture: CaptureRequest? = null

    internal class CaptureRequest(
        val file: File,
        val target: ScanGeometry.ScanTarget,
        val onSaved: (FrameData) -> Unit,
        var attempts: Int = 0
    )

    /** Creates the ARCore session. Returns false when AR is unusable (caller falls back). */
    fun tryCreateSession(ctx: Context): Boolean = try {
        val s = Session(ctx)
        val config = Config(s).apply {
            focusMode = Config.FocusMode.AUTO
            lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            updateMode = Config.UpdateMode.BLOCKING
        }
        s.configure(config)
        pickHighestResCameraConfig(s)
        session = s
        readDisplayAndSensorRotation(ctx, s)
        true
    } catch (t: Throwable) {
        Log.e(TAG, "ARCore session creation failed", t)
        session = null
        false
    }

    /** Prefer the highest-resolution supported camera configuration for sharper stitches. */
    private fun pickHighestResCameraConfig(s: Session) = try {
        s.supportedCameraConfigs.maxByOrNull { it.imageSize.width * it.imageSize.height }?.let { s.cameraConfig = it }
    } catch (_: Throwable) {
    }

    private fun readDisplayAndSensorRotation(ctx: Context, s: Session) {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        displayRotation = wm.defaultDisplay.rotation
        captureRotationDeg = 90f
        try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val arCameraId = s.cameraConfig.cameraId
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val sensorDeg = (chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90).toFloat()
                    val displayDeg = displayRotation * 90f
                    captureRotationDeg = ((sensorDeg - displayDeg) % 360f + 360f) % 360f
                    // Prefer the exact camera ARCore picked when ids match.
                    if (id == arCameraId) break
                }
            }
        } catch (_: Throwable) {
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun createView(ctx: Context): GLSurfaceView {
        val view = GLSurfaceView(ctx)
        view.setEGLContextClientVersion(2)
        view.preserveEGLContextOnPause = true
        view.setRenderer(ArBackgroundRenderer(this))
        view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glView = view
        return view
    }

    fun resume() {
        try { session?.resume() } catch (t: Throwable) { Log.e(TAG, "AR resume failed", t) }
        glView?.onResume()
    }

    fun pause() {
        glView?.onPause()
        try { session?.pause() } catch (_: Throwable) {}
    }

    fun shutdown() {
        glView = null
        executor.shutdown()
        try { session?.close() } catch (_: Throwable) {}
        session = null
    }

    /** Requests a still capture of the current AR camera frame, tagged with the scan slot. */
    fun requestCapture(file: File, target: ScanGeometry.ScanTarget, onSaved: (FrameData) -> Unit) {
        if (session != null) pendingCapture = CaptureRequest(file, target, onSaved)
    }

    internal fun runOnWorker(block: () -> Unit) { executor.execute(block) }

    internal fun postStatus(status: ArSphereStatus) {
        lastHeading = status.headingDeg
        lastPitch = status.pitchDeg
        mainHandler.post { onStatus?.invoke(status) }
    }

    internal fun postError(msg: String) {
        mainHandler.post {
            onStatus?.invoke(ArSphereStatus(false, lastHeading, lastPitch, false, false, 0, false, msg))
        }
    }

    internal fun postOnMain(block: () -> Unit) { mainHandler.post(block) }
}

// ═════════════════════════════════════════════════════════════
//  GL RENDERER — camera background + per-frame AR understanding
// ═════════════════════════════════════════════════════════════

private class ArBackgroundRenderer(private val engine: ArSphereCaptureEngine) : GLSurfaceView.Renderer {

    private val quadCoords: FloatBuffer = directBuffer(
        floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
    )
    private val quadTexCoordsSrc: FloatBuffer = directBuffer(
        floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)
    )
    private val frameUvs: FloatBuffer =
        ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private var program = 0
    private var posAttrib = 0
    private var uvAttrib = 0
    private var texUniform = 0
    private var cameraTexture = -1
    private var width = 1
    private var height = 1
    private var textureBoundToSession = false

    // Yaw unwrapping so captured headings stay continuous (no 0°/360° wrap jumps).
    private var lastRawYaw: Float? = null
    private var cumulativeYaw = 0f

    override fun onSurfaceCreated(_gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0.04f, 0.06f, 0.08f, 1f)
        cameraTexture = createOesTexture()
        engine.session?.setCameraTextureName(cameraTexture)
        program = buildShaderProgram()
        posAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        uvAttrib = GLES20.glGetAttribLocation(program, "a_TexCoord")
        texUniform = GLES20.glGetUniformLocation(program, "sTexture")
        GLES20.glFrontFace(GLES20.GL_CW)
    }

    override fun onSurfaceChanged(_gl: javax.microedition.khronos.opengles.GL10?, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
        width = w
        height = h
    }

    override fun onDrawFrame(_gl: javax.microedition.khronos.opengles.GL10?) {
        val session = engine.session
        if (session == null) { clearScreen(); return }
        try {
            // The GL surface can be created before the session exists — bind lazily.
            if (!textureBoundToSession) {
                session.setCameraTextureName(cameraTexture)
                textureBoundToSession = true
            }
            session.setDisplayGeometry(engine.displayRotation, width, height)
            val frame = session.update()
            val camera = frame.camera

            if (camera.trackingState != TrackingState.TRACKING) {
                clearScreen()
                engine.postStatus(
                    ArSphereStatus(
                        active = false,
                        headingDeg = engine.lastHeading,
                        pitchDeg = engine.lastPitch,
                        floorFound = false,
                        ceilingFound = false,
                        wallsFound = 0,
                        lowLight = false,
                        failureMsg = failureText(camera.trackingFailureReason)
                    )
                )
                return
            }

            // ── Device orientation → heading / pitch ──────────────────
            val pose = camera.displayOrientedPose
            val viewVec = pose.zAxis
            // AR camera looks down -Z; view direction in world:
            val viewX = -viewVec[0]
            val viewY = -viewVec[1]
            val viewZ = -viewVec[2]
            val pitchDeg = Math.toDegrees(asin(viewY.coerceIn(-1f, 1f)).toDouble()).toFloat()
            val rawYaw = Math.toDegrees(atan2(viewX, viewZ).toDouble()).toFloat()
            cumulativeYaw = unwrapYaw(rawYaw)
            val headingDeg = ((cumulativeYaw % 360f) + 360f) % 360f

            // ── Room understanding: floor / ceiling / walls ───────────
            var floorFound = false
            var ceilingFound = false
            var wallsFound = 0
            for (p in session.getAllTrackables(Plane::class.java)) {
                if (p.trackingState != TrackingState.TRACKING) continue
                when (p.type) {
                    Plane.Type.HORIZONTAL_UPWARD_FACING -> floorFound = true
                    Plane.Type.HORIZONTAL_DOWNWARD_FACING -> ceilingFound = true
                    Plane.Type.VERTICAL -> wallsFound++
                    else -> {}
                }
            }

            // ── Light level (drives the auto light-adjust warning) ────
            val le = frame.lightEstimate
            val lowLight = le.state == LightEstimate.State.VALID && le.pixelIntensity < 0.3f

            // ── Still capture (AR camera image, not a second camera) ──
            engine.pendingCapture?.let { cap ->
                engine.pendingCapture = null
                performCapture(frame, cap, headingDeg, pitchDeg)
            }

            drawBackground(frame)
            engine.postStatus(
                ArSphereStatus(true, headingDeg, pitchDeg, floorFound, ceilingFound, wallsFound, lowLight, null)
            )
        } catch (t: Throwable) {
            Log.e("ArSphereScanner", "AR frame failed", t)
            clearScreen()
            engine.postError("AR error: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    private fun unwrapYaw(raw: Float): Float {
        val last = lastRawYaw
        lastRawYaw = raw
        if (last == null) { cumulativeYaw = 0f; return 0f }
        var d = raw - last
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        cumulativeYaw += d
        return cumulativeYaw
    }

    private fun failureText(reason: TrackingFailureReason): String? = when (reason) {
        TrackingFailureReason.INSUFFICIENT_LIGHT -> "scan_low_light"
        TrackingFailureReason.EXCESSIVE_MOTION -> "scan_slow_down"
        TrackingFailureReason.INSUFFICIENT_FEATURES -> "scan_localize"
        else -> null
    }

    // ── Capture: YUV_420_888 → NV21 → JPEG (rotated to display up) ──
    private fun performCapture(frame: Frame, cap: ArSphereCaptureEngine.CaptureRequest, heading: Float, pitch: Float) {
        try {
            val image = frame.acquireCameraImage()
            val w = image.width
            val h = image.height
            val nv21 = yuv420ToNv21(image)
            image.close()

            val rotation = engine.captureRotationDeg
            val file = cap.file
            engine.runOnWorker {
                try {
                    val yuv = YuvImage(nv21, ImageFormat.NV21, w, h, null)
                    val jpegBytes = ByteArrayOutputStream().also { out ->
                        yuv.compressToJpeg(Rect(0, 0, w, h), 92, out)
                    }.toByteArray()
                    var bmp = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                    val matrix = Matrix().apply { postRotate(rotation) }
                    val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    if (rotated != bmp) bmp.recycle()
                    FileOutputStream(file).use { rotated.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                    rotated.recycle()
                    val fd = FrameData(
                        path = file.absolutePath,
                        heading = heading,
                        pitchDeg = pitch,
                        row = cap.target.ringIndex,
                        col = cap.target.targetIndex,
                        isCap = cap.target.isCap,
                        capType = cap.target.capType
                    )
                    engine.postOnMain { cap.onSaved(fd) }
                } catch (t: Throwable) {
                    Log.e("ArSphereScanner", "Capture save failed", t)
                }
            }
        } catch (_: NotYetAvailableException) {
            if (cap.attempts < 20) {
                cap.attempts++
                engine.pendingCapture = cap
            }
        } catch (t: Throwable) {
            Log.e("ArSphereScanner", "Capture failed", t)
        }
    }

    // ── Camera background drawing ─────────────────────────────
    private fun drawBackground(frame: Frame) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (cameraTexture < 0 || program == 0) return
        try {
            frame.transformDisplayUvCoords(quadTexCoordsSrc, frameUvs)
        } catch (_: Throwable) {
            return
        }
        frameUvs.position(0)
        quadCoords.position(0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture)
        GLES20.glUseProgram(program)
        GLES20.glUniform1i(texUniform, 0)
        GLES20.glEnableVertexAttribArray(posAttrib)
        GLES20.glVertexAttribPointer(posAttrib, 2, GLES20.GL_FLOAT, false, 0, quadCoords)
        GLES20.glEnableVertexAttribArray(uvAttrib)
        GLES20.glVertexAttribPointer(uvAttrib, 2, GLES20.GL_FLOAT, false, 0, frameUvs)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(posAttrib)
        GLES20.glDisableVertexAttribArray(uvAttrib)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    private fun clearScreen() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    }

    private fun createOesTexture(): Int {
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return tex[0]
    }

    private fun buildShaderProgram(): Int {
        val vertex = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
              gl_Position = a_Position;
              v_TexCoord = a_TexCoord;
            }
        """.trimIndent()
        val fragment = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
              gl_FragColor = texture2D(sTexture, v_TexCoord);
            }
        """.trimIndent()
        fun compile(type: Int, src: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, src)
            GLES20.glCompileShader(shader)
            return shader
        }
        val vs = compile(GLES20.GL_VERTEX_SHADER, vertex)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return prog
    }

    private fun directBuffer(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(values)
            position(0)
        }
}

/** YUV_420_888 camera image → NV21 byte array. */
private fun yuv420ToNv21(image: android.media.Image): ByteArray {
    val width = image.width
    val height = image.height
    val ySize = width * height
    val out = ByteArray(ySize + ySize / 2)

    val yPlane = image.planes[0]
    val yBuf = yPlane.buffer
    if (yPlane.rowStride == width) {
        yBuf.get(out, 0, ySize)
    } else {
        var pos = 0
        for (row in 0 until height) {
            yBuf.position(row * yPlane.rowStride)
            yBuf.get(out, pos, width)
            pos += width
        }
    }

    // Interleave V then U (NV21 order), handling row/pixel strides.
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]
    val uBuf = uPlane.buffer
    val vBuf = vPlane.buffer
    var pos = ySize
    val chromaWidth = width / 2
    val chromaHeight = height / 2
    for (row in 0 until chromaHeight) {
        val vRow = row * vPlane.rowStride
        val uRow = row * uPlane.rowStride
        for (col in 0 until chromaWidth) {
            out[pos++] = vBuf.get(vRow + col * vPlane.pixelStride)
            out[pos++] = uBuf.get(uRow + col * uPlane.pixelStride)
        }
    }
    return out
}

// ═════════════════════════════════════════════════════════════
//  COMPOSABLE — clean AR-guided spherical capture UI
// ═════════════════════════════════════════════════════════════

private enum class ArLaunchState { CHECKING, INSTALL_PROMPT, RUNNING, FALLBACK }

@Composable
internal fun ArSphereCapturePhase(
    roomName: String,
    scanMode: ScanGeometry.ScanMode,
    scanTargets: List<ScanGeometry.ScanTarget>,
    currentTargetIdx: Int,
    capturedFrames: SnapshotStateList<FrameData>,
    gyroHeading: Float,
    gyroPitch: Float,
    onFrameCaptured: (FrameData) -> Unit,
    onRequestLegacyFallback: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val engine = remember { ArSphereCaptureEngine() }
    var launchState by remember { mutableStateOf(ArLaunchState.CHECKING) }
    var status by remember { mutableStateOf<ArSphereStatus?>(null) }
    val capturedCallback by rememberUpdatedState(onFrameCaptured)
    val onStopLatest by rememberUpdatedState(onStop)
    val onBackLatest by rememberUpdatedState(onBack)
    val legacyFallback by rememberUpdatedState(onRequestLegacyFallback)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        val avail = ArSphereCaptureEngine.checkAvailability(ctx)
        launchState = when {
            avail.isSupported() && avail != ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ArLaunchState.RUNNING
            avail.isSupported() -> ArLaunchState.INSTALL_PROMPT
            else -> ArLaunchState.FALLBACK
        }
    }

    LaunchedEffect(launchState) {
        if (launchState == ArLaunchState.RUNNING && engine.session == null) {
            if (!engine.tryCreateSession(ctx)) launchState = ArLaunchState.FALLBACK
        }
    }

    DisposableEffect(launchState) {
        if (launchState == ArLaunchState.RUNNING) {
            engine.onStatus = { status = it }
            engine.resume()
        }
        onDispose {
            engine.onStatus = null
            if (launchState == ArLaunchState.RUNNING) engine.pause()
        }
    }

    DisposableEffect(lifecycleOwner, launchState) {
        if (launchState != ArLaunchState.RUNNING) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> engine.resume()
                Lifecycle.Event.ON_PAUSE -> engine.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose { engine.shutdown() }
    }

    // ── Derived guidance state ────────────────────────────────────
    val target = scanTargets.getOrNull(currentTargetIdx) ?: scanTargets.last()
    val arActive = launchState == ArLaunchState.RUNNING && status?.active == true
    val heading = if (arActive) status?.headingDeg ?: gyroHeading else gyroHeading
    val pitch = if (arActive) status?.pitchDeg ?: gyroPitch else gyroPitch

    val pitchError = pitch - target.pitchDeg
    val headingError = ((heading - target.headingDeg + 540) % 360) - 180
    val aligned = abs(pitchError) <= 10f && (target.isCap || abs(headingError) <= 10f)
    val alreadyCaptured = capturedFrames.any { it.col == target.targetIndex }

    // Auto-capture when aligned (AR only — real camera frames).
    LaunchedEffect(aligned, currentTargetIdx, capturedFrames.size, arActive) {
        if (aligned && arActive && !alreadyCaptured) {
            delay(650)
            val stillTarget = scanTargets.getOrNull(currentTargetIdx) ?: return@LaunchedEffect
            val stillAligned = abs((status?.pitchDeg ?: gyroPitch) - stillTarget.pitchDeg) <= 10f &&
                (stillTarget.isCap || abs(((status?.headingDeg ?: gyroHeading) - stillTarget.headingDeg + 540) % 360 - 180) <= 10f)
            if (stillAligned) {
                val file = File(ctx.cacheDir, "frame_r${stillTarget.ringIndex}_c${stillTarget.targetIndex}_${System.currentTimeMillis()}.jpg")
                engine.requestCapture(file, stillTarget) { fd -> capturedCallback(fd) }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (launchState) {
            ArLaunchState.CHECKING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(L("scan_localize"), color = Color.White, fontSize = 13.sp)
            }

            ArLaunchState.INSTALL_PROMPT -> ArInstallCard(onInstall = {
                val activity = ctx as? Activity
                if (activity != null && ArSphereCaptureEngine.requestInstall(activity)) {
                    launchState = ArLaunchState.CHECKING
                } else {
                    launchState = ArLaunchState.FALLBACK
                }
            }, onFallback = { launchState = ArLaunchState.FALLBACK })

            ArLaunchState.FALLBACK -> ArFallbackNotice()

            ArLaunchState.RUNNING -> AndroidView(
                factory = { c -> engine.createView(c) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── AR guidance overlay (draws over camera feed or fallback) ──
        ArGuidanceOverlay(
            roomName = roomName,
            scanTargets = scanTargets,
            currentTargetIdx = currentTargetIdx,
            capturedFrames = capturedFrames,
            heading = heading,
            pitch = pitch,
            arActive = arActive,
            floorFound = status?.floorFound == true,
            ceilingFound = status?.ceilingFound == true,
            lowLight = status?.lowLight == true,
            failureMsg = status?.failureMsg,
            target = target,
            aligned = aligned,
            alreadyCaptured = alreadyCaptured,
            fallbackMode = launchState != ArLaunchState.RUNNING,
            onManualCapture = {
                if (arActive) {
                    val file = File(ctx.cacheDir, "frame_r${target.ringIndex}_c${target.targetIndex}_${System.currentTimeMillis()}.jpg")
                    engine.requestCapture(file, target) { fd -> capturedCallback(fd) }
                } else {
                    // Device can't run ARCore — hand control back to the legacy capture flow.
                    legacyFallback()
                }
            },
            onStop = { onStopLatest() },
            onBack = { onBackLatest() }
        )
    }
}

// ── Overlay: status chips, reticle, next-slot pointer, guidance, controls ──

@Composable
private fun ArGuidanceOverlay(
    roomName: String,
    scanTargets: List<ScanGeometry.ScanTarget>,
    currentTargetIdx: Int,
    capturedFrames: SnapshotStateList<FrameData>,
    heading: Float,
    pitch: Float,
    arActive: Boolean,
    floorFound: Boolean,
    ceilingFound: Boolean,
    lowLight: Boolean,
    failureMsg: String?,
    target: ScanGeometry.ScanTarget,
    aligned: Boolean,
    alreadyCaptured: Boolean,
    fallbackMode: Boolean,
    onManualCapture: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit
) {
    val locale = LocalDorjaLocale.current
    val green = Color(0xFF4CAF50)
    val yellow = Color(0xFFFFC107)

    Box(Modifier.fillMaxSize()) {

        // ── Top bar ──
        Box(
            Modifier.fillMaxWidth().height(70.dp)
                .background(Color.Black.copy(alpha = 0.55f))
                .align(Alignment.TopCenter)
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 34.dp, start = 12.dp, end = 12.dp).align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (fallbackMode) L("scan_ar_fail") else "AR ${L("scan_ar_on")}".uppercase(),
                    color = if (fallbackMode) yellow else green,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(roomName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Box(Modifier.size(38.dp))
        }

        // ── Room-understanding chips (what AR knows about the room) ──
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = 84.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArRoomChip(L("scan_floor_found"), floorFound)
            ArRoomChip(L("scan_ceiling_found"), ceilingFound)
            if (lowLight) ArRoomChip(L("scan_low_light"), false, warn = true)
        }

        // ── Alignment failure hint ──
        if (failureMsg != null && !fallbackMode) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp)
            ) {
                Text(
                    L(failureMsg),
                    color = yellow,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // ── Center reticle ──
        ArReticle(aligned = aligned && !alreadyCaptured, fallback = fallbackMode)

        // ── Next-slot pointer + turn guidance ──
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val pxPerDeg = size.width / 65f

            val next = nextUncapturedTarget(scanTargets, currentTargetIdx, capturedFrames)
            if (next != null && next.targetIndex != target.targetIndex) {
                val relH = ((next.headingDeg - heading + 540) % 360) - 180
                val relP = next.pitchDeg - pitch
                val nx = cx + relH * pxPerDeg
                val ny = cy - relP * pxPerDeg
                val onScreen = nx in 0f..size.width && ny in 0f..size.height
                if (onScreen) {
                    drawCircle(yellow.copy(alpha = 0.9f), 6.dp.toPx(), Offset(nx, ny))
                    drawCircle(yellow.copy(alpha = 0.35f), 12.dp.toPx(), Offset(nx, ny), style = Stroke(1.5.dp.toPx()))
                    drawIntoCanvas { c ->
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        c.nativeCanvas.drawText("NEXT", nx, ny - 16.dp.toPx(), paint)
                    }
                } else {
                    // Small edge chevron pointing toward the next slot.
                    val dx = nx - cx
                    val dy = ny - cy
                    val ang = atan2(dy, dx)
                    val radius = min(size.width, size.height) * 0.40f
                    val ex = cx + radius * cos(ang)
                    val ey = cy + radius * sin(ang)
                    drawContext.canvas.save()
                    drawContext.canvas.translate(ex, ey)
                    drawContext.canvas.rotate(Math.toDegrees(ang.toDouble()).toFloat() + 90f)
                    val s = 8.dp.toPx()
                    val p1 = Offset(0f, -s)
                    val p2 = Offset(-s * 0.7f, s * 0.5f)
                    val p3 = Offset(s * 0.7f, s * 0.5f)
                    drawLine(yellow, p1, p2, 2.5.dp.toPx())
                    drawLine(yellow, p2, p3, 2.5.dp.toPx())
                    drawLine(yellow, p3, p1, 2.5.dp.toPx())
                    drawContext.canvas.restore()
                }
            }
        }

        // ── Big unambiguous turn/tilt guidance ──
        if (!aligned) {
            val pe = pitch - target.pitchDeg
            val he = ((heading - target.headingDeg + 540) % 360) - 180
            val tiltUp = pe < -10f
            val tiltDown = pe > 10f
            val turnLeft = he > 10f && !target.isCap
            val turnRight = he < -10f && !target.isCap
            if (tiltUp || tiltDown || turnLeft || turnRight) {
                Column(
                    Modifier.align(Alignment.Center).offset(y = 110.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DirectionGlyph(tiltUp, tiltDown, turnLeft, turnRight)
                    val deg = when {
                        tiltUp -> abs(pe)
                        tiltDown -> abs(pe)
                        turnLeft -> abs(he)
                        else -> abs(he)
                    }
                    val key = when {
                        tiltUp -> "scan_tilt_up"
                        tiltDown -> "scan_tilt_down"
                        turnLeft -> "scan_turn_left"
                        else -> "scan_turn_right"
                    }
                    Text(
                        "${L(key)} ${"%.0f".format(deg)}°",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ── Bottom controls ──
        Box(
            Modifier.fillMaxWidth().height(150.dp)
                .background(Color.Black.copy(alpha = 0.55f))
                .align(Alignment.BottomCenter)
        )
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val coverage = remember(capturedFrames.size) {
                ScanGeometry.computeCoveragePercent(capturedFrames.map { Pair(it.heading, it.pitchDeg) })
            }
            Text(
                locale.format("scan_shot_progress", currentTargetIdx + 1, scanTargets.size) + " • ${"%.0f".format(coverage)}%",
                color = if (aligned) green else Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    alreadyCaptured -> "✓ ${L("scan_tilt_ok")}"
                    aligned && arActive -> L("scan_hold")
                    aligned -> L("scan_tilt_ok")
                    target.isCap && target.capType == "zenith" -> L("scan_tilt_up")
                    target.isCap && target.capType == "nadir" -> L("scan_tilt_down")
                    else -> L("scan_move_next")
                },
                color = when {
                    alreadyCaptured -> green
                    aligned -> yellow
                    else -> Color.White
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                ArShotCounter(capturedFrames)
                Spacer(Modifier.width(18.dp))
                Box(
                    Modifier.size(68.dp).clip(CircleShape)
                        .background(if (aligned) green.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f))
                        .border(3.dp, if (aligned) green else Color.White, CircleShape)
                        .clickable { onManualCapture() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(if (aligned) green else Color.White.copy(alpha = 0.9f)))
                }
                Spacer(Modifier.width(18.dp))
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE53935))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onStop() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
                }
            }
        }
    }
}

@Composable
private fun ArShotCounter(frames: SnapshotStateList<FrameData>) {
    Box(Modifier.size(52.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.Black.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (frames.isEmpty()) Color.White.copy(alpha = 0.25f) else Color(0xFF4CAF50)),
            modifier = Modifier.fillMaxSize()
        ) {
            if (frames.isEmpty()) {
                Box(contentAlignment = Alignment.Center) {
                    Text("0", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                val lastPath = frames.last().path
                if (lastPath.isNotBlank() && File(lastPath).exists()) {
                    val bmp = remember(lastPath) { BitmapFactory.decodeFile(lastPath) }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Last captured frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${frames.size}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${frames.size}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (frames.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
                    .border(2.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${frames.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ArRoomChip(label: String, ok: Boolean, warn: Boolean = false) {
    val color = when {
        warn -> Color(0xFFFFC107)
        ok -> Color(0xFF4CAF50)
        else -> Color.White.copy(alpha = 0.45f)
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(label, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ArReticle(aligned: Boolean, fallback: Boolean) {
    val pulse = rememberInfiniteTransition(label = "reticle")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (aligned) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "reticleScale"
    )
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = 44.dp.toPx() * scale
        val color = if (aligned) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.85f)
        drawCircle(color.copy(alpha = 0.25f), r * 1.25f, Offset(cx, cy))
        drawCircle(color, r, Offset(cx, cy), style = Stroke(2.5.dp.toPx()))
        drawCircle(color, 4.dp.toPx(), Offset(cx, cy))
        if (!fallback) {
            for (i in 0 until 4) {
                val a = Math.toRadians((i * 90 + 45).toDouble())
                val inner = r * 1.1f
                val outer = r * 1.22f
                drawLine(
                    color.copy(alpha = 0.6f),
                    Offset(cx + inner * cos(a).toFloat(), cy + inner * sin(a).toFloat()),
                    Offset(cx + outer * cos(a).toFloat(), cy + outer * sin(a).toFloat()),
                    2.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun DirectionGlyph(tiltUp: Boolean, tiltDown: Boolean, turnLeft: Boolean, turnRight: Boolean) {
    Canvas(Modifier.size(54.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val s = size.minDimension / 4f
        fun arrow(dir: Offset, color: Color) {
            val tip = Offset(c.x + dir.x * s, c.y + dir.y * s)
            val base = Offset(c.x + dir.x * s * 0.2f, c.y + dir.y * s * 0.2f)
            val perp = Offset(-dir.y, dir.x)
            drawLine(color, base, tip, 5.dp.toPx())
            drawLine(color, tip, Offset(tip.x - dir.x * s * 0.5f + perp.x * s * 0.35f, tip.y - dir.y * s * 0.5f + perp.y * s * 0.35f), 5.dp.toPx())
            drawLine(color, tip, Offset(tip.x - dir.x * s * 0.5f - perp.x * s * 0.35f, tip.y - dir.y * s * 0.5f - perp.y * s * 0.35f), 5.dp.toPx())
        }
        val yellow = Color(0xFFFFC107)
        when {
            tiltUp -> arrow(Offset(0f, -1f), yellow)
            tiltDown -> arrow(Offset(0f, 1f), yellow)
            turnLeft -> arrow(Offset(-1f, 0f), yellow)
            turnRight -> arrow(Offset(1f, 0f), yellow)
        }
    }
}

private fun nextUncapturedTarget(
    scanTargets: List<ScanGeometry.ScanTarget>,
    currentTargetIdx: Int,
    capturedFrames: List<FrameData>
): ScanGeometry.ScanTarget? {
    val capturedCols = capturedFrames.map { it.col }.toSet()
    // First uncaptured slot after the current one, else wrap to the first uncaptured.
    scanTargets.drop(currentTargetIdx + 1).firstOrNull { it.targetIndex !in capturedCols }?.let { return it }
    return scanTargets.firstOrNull { it.targetIndex !in capturedCols }
}

@Composable
private fun ArInstallCard(onInstall: () -> Unit, onFallback: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0A1418),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00BCD4).copy(alpha = 0.4f)),
            modifier = Modifier.padding(24.dp).fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFFFC107), modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(10.dp))
                Text(L("scan_ar_install_title"), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(L("scan_ar_install_hint"), color = Color(0xFFB0BEC5), fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.clickable { onInstall() }
                ) {
                    Text(
                        L("scan_ar_install_cta"),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    L("scan_ar_use_motion"),
                    color = Color(0xFF00BCD4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onFallback() }
                )
            }
        }
    }
}

@Composable
private fun ArFallbackNotice() {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0F12)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFFFC107), modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(L("scan_ar_fail"), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(L("scan_instruction"), color = Color(0xFFB0BEC5), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}
