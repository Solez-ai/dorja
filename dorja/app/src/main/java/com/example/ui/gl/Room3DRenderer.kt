package com.example.ui.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Room3DRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // Projection and View matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Camera angles
    var panAngle = 0f
    var tiltAngle = 0f
    var roomType = "LIVING_ROOM"
    var transitionAlpha = 1.0f

    // Shader program
    private var programId = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0

    // Vertex Buffers
    private lateinit var floorVertexBuffer: FloatBuffer
    private lateinit var ceilingVertexBuffer: FloatBuffer
    private lateinit var backWallVertexBuffer: FloatBuffer
    private lateinit var leftWallVertexBuffer: FloatBuffer
    private lateinit var rightWallVertexBuffer: FloatBuffer
    private lateinit var windowVertexBuffer: FloatBuffer
    private lateinit var doorVertexBuffer: FloatBuffer
    private lateinit var gridVertexBuffer: FloatBuffer
    private lateinit var furnitureVertexBuffer: FloatBuffer

    private var gridVertexCount = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.043f, 0.121f, 0.2f, 1.0f) // Dorja Ink950
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        setupShaders()
        buildGeometry()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 65f, ratio, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(programId)

        // Setup View Matrix
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)

        // Apply Rotations (Look around pan & tilt)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, tiltAngle, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, panAngle, 0f, 1f, 0f)

        // Calculate MVP
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Colors tailored to Room Type
        val (wallColor, floorColor) = getRoomColors(roomType)

        // 1. Draw Floor
        drawQuad(floorVertexBuffer, floorColor)

        // 2. Draw Floor Grid
        drawGrid()

        // 3. Draw Ceiling
        drawQuad(ceilingVertexBuffer, floatArrayOf(0.95f, 0.94f, 0.92f, 1.0f))

        // 4. Draw Back Wall
        drawQuad(backWallVertexBuffer, wallColor)

        // 5. Draw Left Wall
        drawQuad(leftWallVertexBuffer, floatArrayOf(wallColor[0] * 0.9f, wallColor[1] * 0.9f, wallColor[2] * 0.9f, 1.0f))

        // 6. Draw Right Wall
        drawQuad(rightWallVertexBuffer, floatArrayOf(wallColor[0] * 0.85f, wallColor[1] * 0.85f, wallColor[2] * 0.85f, 1.0f))

        // 7. Draw Window with Outdoor Daylight on Back Wall
        drawQuad(windowVertexBuffer, floatArrayOf(0.85f, 0.95f, 1.0f, 1.0f))

        // 8. Draw Doorway Hotspot on Right Wall
        drawQuad(doorVertexBuffer, floatArrayOf(0.0f, 0.486f, 0.471f, 1.0f)) // Jol600 teal door

        // 9. Draw Furniture Silhouette
        drawFurniture()
    }

    private fun getRoomColors(type: String): Pair<FloatArray, FloatArray> {
        return when (type) {
            "BEDROOM" -> Pair(
                floatArrayOf(0.91f, 0.89f, 0.85f, 1.0f), // Warm cozy wall
                floatArrayOf(0.55f, 0.42f, 0.32f, 1.0f)  // Wood floor
            )
            "KITCHEN" -> Pair(
                floatArrayOf(0.88f, 0.92f, 0.92f, 1.0f), // Clean tile wall
                floatArrayOf(0.75f, 0.76f, 0.78f, 1.0f)  // Gray stone floor
            )
            "BATHROOM" -> Pair(
                floatArrayOf(0.82f, 0.90f, 0.90f, 1.0f), // Turquoise tile
                floatArrayOf(0.65f, 0.72f, 0.75f, 1.0f)  // Anti-slip tile
            )
            else -> Pair(
                floatArrayOf(0.96f, 0.94f, 0.90f, 1.0f), // Light paper wall
                floatArrayOf(0.68f, 0.64f, 0.58f, 1.0f)  // Sandstone floor
            )
        }
    }

    private fun drawQuad(buffer: FloatBuffer, color: FloatArray) {
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer)

        // Apply alpha transition
        val finalColor = floatArrayOf(color[0], color[1], color[2], color[3] * transitionAlpha)
        GLES20.glUniform4fv(colorHandle, 1, finalColor, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun drawGrid() {
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, gridVertexBuffer)
        GLES20.glUniform4fv(colorHandle, 1, floatArrayOf(0.3f, 0.3f, 0.3f, 0.4f * transitionAlpha), 0)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, gridVertexCount)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun drawFurniture() {
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, furnitureVertexBuffer)
        GLES20.glUniform4fv(colorHandle, 1, floatArrayOf(0.043f, 0.121f, 0.2f, 0.85f * transitionAlpha), 0) // Ink950
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 24)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun setupShaders() {
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(programId, "vPosition")
        colorHandle = GLES20.glGetUniformLocation(programId, "vColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private fun buildGeometry() {
        val roomW = 3.5f
        val roomH = 2.5f
        val roomD = 4.0f

        // Floor: y = -roomH
        val floorVertices = floatArrayOf(
            -roomW, -roomH,  roomD,
             roomW, -roomH,  roomD,
             roomW, -roomH, -roomD,
            -roomW, -roomH, -roomD
        )
        floorVertexBuffer = createFloatBuffer(floorVertices)

        // Ceiling: y = +roomH
        val ceilingVertices = floatArrayOf(
            -roomW,  roomH, -roomD,
             roomW,  roomH, -roomD,
             roomW,  roomH,  roomD,
            -roomW,  roomH,  roomD
        )
        ceilingVertexBuffer = createFloatBuffer(ceilingVertices)

        // Back Wall: z = -roomD
        val backWallVertices = floatArrayOf(
            -roomW, -roomH, -roomD,
             roomW, -roomH, -roomD,
             roomW,  roomH, -roomD,
            -roomW,  roomH, -roomD
        )
        backWallVertexBuffer = createFloatBuffer(backWallVertices)

        // Left Wall: x = -roomW
        val leftWallVertices = floatArrayOf(
            -roomW, -roomH,  roomD,
            -roomW, -roomH, -roomD,
            -roomW,  roomH, -roomD,
            -roomW,  roomH,  roomD
        )
        leftWallVertexBuffer = createFloatBuffer(leftWallVertices)

        // Right Wall: x = +roomW
        val rightWallVertices = floatArrayOf(
             roomW, -roomH, -roomD,
             roomW, -roomH,  roomD,
             roomW,  roomH,  roomD,
             roomW,  roomH, -roomD
        )
        rightWallVertexBuffer = createFloatBuffer(rightWallVertices)

        // Window on Back Wall
        val windowVertices = floatArrayOf(
            -1.2f, -0.2f, -roomD + 0.01f,
             1.2f, -0.2f, -roomD + 0.01f,
             1.2f,  1.4f, -roomD + 0.01f,
            -1.2f,  1.4f, -roomD + 0.01f
        )
        windowVertexBuffer = createFloatBuffer(windowVertices)

        // Doorway Hotspot on Right Wall
        val doorVertices = floatArrayOf(
            roomW - 0.01f, -roomH, -1.0f,
            roomW - 0.01f, -roomH,  0.6f,
            roomW - 0.01f,  0.8f,  0.6f,
            roomW - 0.01f,  0.8f, -1.0f
        )
        doorVertexBuffer = createFloatBuffer(doorVertices)

        // Floor Grid Lines
        val gridList = mutableListOf<Float>()
        var x = -roomW
        while (x <= roomW) {
            gridList.addAll(listOf(x, -roomH + 0.01f, -roomD, x, -roomH + 0.01f, roomD))
            x += 0.7f
        }
        var z = -roomD
        while (z <= roomD) {
            gridList.addAll(listOf(-roomW, -roomH + 0.01f, z, roomW, -roomH + 0.01f, z))
            z += 0.7f
        }
        gridVertexCount = gridList.size / 3
        gridVertexBuffer = createFloatBuffer(gridList.toFloatArray())

        // Furniture Wireframe 3D Box (Sofa or Bed)
        val fMinX = -2.2f
        val fMaxX = -0.6f
        val fMinY = -roomH
        val fMaxY = -roomH + 0.8f
        val fMinZ = -2.5f
        val fMaxZ = -1.0f

        val furnitureLines = floatArrayOf(
            // Bottom rectangle
            fMinX, fMinY, fMinZ, fMaxX, fMinY, fMinZ,
            fMaxX, fMinY, fMinZ, fMaxX, fMinY, fMaxZ,
            fMaxX, fMinY, fMaxZ, fMinX, fMinY, fMaxZ,
            fMinX, fMinY, fMaxZ, fMinX, fMinY, fMinZ,
            // Top rectangle
            fMinX, fMaxY, fMinZ, fMaxX, fMaxY, fMinZ,
            fMaxX, fMaxY, fMinZ, fMaxX, fMaxY, fMaxZ,
            fMaxX, fMaxY, fMaxZ, fMinX, fMaxY, fMaxZ,
            fMinX, fMaxY, fMaxZ, fMinX, fMaxY, fMinZ,
            // Pillars
            fMinX, fMinY, fMinZ, fMinX, fMaxY, fMinZ,
            fMaxX, fMinY, fMinZ, fMaxX, fMaxY, fMinZ,
            fMaxX, fMinY, fMaxZ, fMaxX, fMaxY, fMaxZ,
            fMinX, fMinY, fMaxZ, fMinX, fMaxY, fMaxZ
        )
        furnitureVertexBuffer = createFloatBuffer(furnitureLines)
    }

    private fun createFloatBuffer(array: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(array.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(array)
                position(0)
            }
    }
}
