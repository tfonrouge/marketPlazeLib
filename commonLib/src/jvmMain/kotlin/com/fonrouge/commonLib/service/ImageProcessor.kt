package com.fonrouge.commonLib.service

import com.fonrouge.commonLib.model.ImageSize
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

enum class WatermarkPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER }

class ImagePipeline {
    private var maxWidth: Int? = null
    private var maxHeight: Int? = null
    private var watermarkText: String? = null
    private var watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT
    private var watermarkOpacity: Float = 0.5f
    private var outputFormat: String = "jpeg"
    private var jpegQuality: Float = 0.85f

    fun resize(maxWidth: Int, maxHeight: Int): ImagePipeline = apply {
        this.maxWidth = maxWidth
        this.maxHeight = maxHeight
    }

    fun resize(imageSize: ImageSize): ImagePipeline = resize(imageSize.maxDimension, imageSize.maxDimension)

    fun watermark(
        text: String,
        position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
        opacity: Float = 0.5f,
    ): ImagePipeline = apply {
        this.watermarkText = text
        this.watermarkPosition = position
        this.watermarkOpacity = opacity
    }

    fun toJpeg(quality: Float = 0.85f): ImagePipeline = apply {
        this.outputFormat = "jpeg"
        this.jpegQuality = quality
    }

    fun toPng(): ImagePipeline = apply {
        this.outputFormat = "png"
    }

    fun process(input: ByteArray): ByteArray {
        var image = ImageIO.read(ByteArrayInputStream(input))
            ?: throw IllegalArgumentException("Cannot decode image from input bytes")

        // Resize (preserving aspect ratio)
        val mw = maxWidth
        val mh = maxHeight
        if (mw != null && mh != null && (image.width > mw || image.height > mh)) {
            val scale = minOf(mw.toDouble() / image.width, mh.toDouble() / image.height)
            val newWidth = (image.width * scale).toInt().coerceAtLeast(1)
            val newHeight = (image.height * scale).toInt().coerceAtLeast(1)
            val resized = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
            val g = resized.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(image, 0, 0, newWidth, newHeight, null)
            g.dispose()
            image = resized
        }

        // Watermark
        val wmText = watermarkText
        if (wmText != null) {
            val g = image.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, watermarkOpacity)
            val fontSize = (image.width / 15).coerceIn(12, 48)
            g.font = Font("SansSerif", Font.BOLD, fontSize)
            val metrics = g.fontMetrics
            val textWidth = metrics.stringWidth(wmText)
            val textHeight = metrics.height
            val margin = 10
            val (x, y) = when (watermarkPosition) {
                WatermarkPosition.TOP_LEFT -> margin to (metrics.ascent + margin)
                WatermarkPosition.TOP_RIGHT -> (image.width - textWidth - margin) to (metrics.ascent + margin)
                WatermarkPosition.BOTTOM_LEFT -> margin to (image.height - margin)
                WatermarkPosition.BOTTOM_RIGHT -> (image.width - textWidth - margin) to (image.height - margin)
                WatermarkPosition.CENTER -> ((image.width - textWidth) / 2) to ((image.height + textHeight) / 2)
            }
            g.color = Color.WHITE
            g.drawString(wmText, x + 1, y + 1)
            g.color = Color.DARK_GRAY
            g.drawString(wmText, x, y)
            g.dispose()
        }

        // Encode output
        val out = ByteArrayOutputStream()
        if (outputFormat == "jpeg") {
            val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
            val param = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = jpegQuality
            }
            writer.output = ImageIO.createImageOutputStream(out)
            writer.write(null, IIOImage(image, null, null), param)
            writer.dispose()
        } else {
            ImageIO.write(image, outputFormat, out)
        }
        return out.toByteArray()
    }
}

object ImageProcessor {
    fun pipeline(): ImagePipeline = ImagePipeline()
}
