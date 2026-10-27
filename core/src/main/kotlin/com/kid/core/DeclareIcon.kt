package com.kid.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.util.Base64

private val LOCAL_HOST = Regex("""^(localhost|.*\.localhost|.*\.local|.*\.test|\[.*]|\d+\.\d+\.\d+\.\d+)$""", RegexOption.IGNORE_CASE)

/**
 * The icon as declare sends it. A public https link goes as is for k-ID to fetch; anything k-ID cannot reach (an image
 * in the app, a local dev server) is read here and sent inline as a data URL.
 */
internal suspend fun declaredIconUrl(iconUrl: String, transport: KIDTransport, readImage: (suspend (URI) -> ByteArray)?): String {
    if (iconUrl.startsWith("data:") || iconUrl.isBlank()) return iconUrl
    val uri = runCatching { URI(iconUrl) }.getOrNull()?.takeIf { it.scheme != null } ?: File(iconUrl).toURI()
    val host = uri.host.orEmpty()
    if (uri.scheme == "https" && host.isNotEmpty() && !LOCAL_HOST.matches(host)) return iconUrl
    val bytes = try {
        when {
            uri.scheme == "http" || uri.scheme == "https" ->
                transport(HttpRequest("GET", uri.toString(), emptyMap(), null)).takeIf { it.status in 200..299 }?.body
            readImage != null -> readImage(uri)
            uri.scheme == "file" -> withContext(Dispatchers.IO) { File(uri).readBytes() }
            else -> null
        }
    } catch (error: Exception) {
        rethrowCancellation(error)
        null
    } ?: throw KIDError(0, KIDErrorCode.INVALID_REQUEST, "Could not read the app icon at $iconUrl.")
    return "data:${imageType(bytes)};base64,${Base64.getEncoder().encodeToString(bytes)}"
}

private fun imageType(bytes: ByteArray): String = when {
    bytes.size >= 4 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() ->
        "image/png"
    bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
    else -> "application/octet-stream"
}
