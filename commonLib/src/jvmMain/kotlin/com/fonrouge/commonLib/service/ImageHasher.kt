package com.fonrouge.commonLib.service

import java.security.MessageDigest

object ImageHasher {
    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

fun ByteArray.isValidImage(): Boolean {
    if (size < 4) return false
    // PNG: 89 50 4E 47
    if (this[0] == 0x89.toByte() && this[1] == 0x50.toByte() && this[2] == 0x4E.toByte() && this[3] == 0x47.toByte()) return true
    // JPEG: FF D8 FF
    if (this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte() && this[2] == 0xFF.toByte()) return true
    // WebP: RIFF....WEBP
    if (size >= 12 && this[0] == 0x52.toByte() && this[1] == 0x49.toByte() && this[8] == 0x57.toByte() && this[9] == 0x45.toByte()) return true
    return false
}

fun ByteArray.imageDimensions(): Pair<Int, Int> {
    val image = javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(this))
        ?: throw IllegalArgumentException("Cannot decode image")
    return image.width to image.height
}
