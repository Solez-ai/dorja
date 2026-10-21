package com.example.scanner.photosphere

import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

/**
 * Injects Google Photo Sphere (`GPano`) XMP metadata into a JPEG file so that both full 360x180
 * spheres and Horizon Ring panoramas (`latitudeSpanDegrees < 180`) carry exact spherical bounds.
 */
object GPanoXmpInjector {

    private const val XMP_NAMESPACE = "http://ns.adobe.com/xap/1.0/\u0000"

    fun inject(
        jpegFile: File,
        croppedWidthPx: Int,
        croppedHeightPx: Int,
        longitudeSpanDegrees: Float = 360f,
        latitudeSpanDegrees: Float = 180f,
        centerLatitudeDegrees: Float = 0f,
    ): Boolean = runCatching {
        if (!jpegFile.exists() || croppedWidthPx <= 0 || croppedHeightPx <= 0) return false
        val bytes = jpegFile.readBytes()
        if (bytes.size < 4 ||
            (bytes[0].toInt() and 0xFF) != 0xFF ||
            (bytes[1].toInt() and 0xFF) != 0xD8
        ) {
            return false
        }

        val lonSpan = longitudeSpanDegrees.coerceIn(1f, 360f)
        val latSpan = latitudeSpanDegrees.coerceIn(1f, 180f)
        val fullWidthPx = ((croppedWidthPx * (360f / lonSpan)).roundToInt()).coerceAtLeast(croppedWidthPx)
        val fullHeightPx = ((fullWidthPx / 2f).roundToInt()).coerceAtLeast(croppedHeightPx)
        val croppedLeftPx = ((fullWidthPx - croppedWidthPx) / 2).coerceAtLeast(0)
        val topLatitude = (centerLatitudeDegrees + latSpan / 2f).coerceIn(-90f, 90f)
        val croppedTopPx = (((90f - topLatitude) / 180f) * fullHeightPx)
            .roundToInt()
            .coerceIn(0, (fullHeightPx - croppedHeightPx).coerceAtLeast(0))

        val xmpXml = buildXmpPacket(
            croppedWidthPx = croppedWidthPx,
            croppedHeightPx = croppedHeightPx,
            fullWidthPx = fullWidthPx,
            fullHeightPx = fullHeightPx,
            croppedLeftPx = croppedLeftPx,
            croppedTopPx = croppedTopPx,
        )
        val namespaceBytes = XMP_NAMESPACE.toByteArray(Charsets.UTF_8)
        val xmlBytes = xmpXml.toByteArray(Charsets.UTF_8)
        val payloadLength = namespaceBytes.size + xmlBytes.size + 2
        if (payloadLength > 0xFFFF) return false

        val out = ByteArrayOutputStream(bytes.size + payloadLength + 4)
        // SOI marker
        out.write(0xFF)
        out.write(0xD8)
        // APP1 marker for XMP
        out.write(0xFF)
        out.write(0xE1)
        out.write((payloadLength ushr 8) and 0xFF)
        out.write(payloadLength and 0xFF)
        out.write(namespaceBytes)
        out.write(xmlBytes)
        // Rest of JPEG after SOI
        out.write(bytes, 2, bytes.size - 2)

        val tempFile = File(jpegFile.parentFile, "${jpegFile.name}.xmp.tmp")
        tempFile.writeBytes(out.toByteArray())
        if (!tempFile.renameTo(jpegFile)) {
            tempFile.copyTo(jpegFile, overwrite = true)
            tempFile.delete()
        }
        true
    }.getOrDefault(false)

    private fun buildXmpPacket(
        croppedWidthPx: Int,
        croppedHeightPx: Int,
        fullWidthPx: Int,
        fullHeightPx: Int,
        croppedLeftPx: Int,
        croppedTopPx: Int,
    ): String = """
        <?xpacket begin="&#xFEFF;" id="W5M0MpCehiHzreSzNTczkc9d"?>
        <x:xmpmeta xmlns:x="adobe:ns:meta/">
          <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description rdf:about=""
                xmlns:GPano="http://ns.google.com/photos/1.0/panorama/"
                GPano:UsePanoramaViewer="True"
                GPano:ProjectionType="equirectangular"
                GPano:CroppedAreaImageWidthPixels="$croppedWidthPx"
                GPano:CroppedAreaImageHeightPixels="$croppedHeightPx"
                GPano:FullPanoWidthPixels="$fullWidthPx"
                GPano:FullPanoHeightPixels="$fullHeightPx"
                GPano:CroppedAreaLeftPixels="$croppedLeftPx"
                GPano:CroppedAreaTopPixels="$croppedTopPx"
                GPano:InitialHorizontalFOVDegrees="75.0"/>
          </rdf:RDF>
        </x:xmpmeta>
        <?xpacket end="w"?>
    """.trimIndent()
}
