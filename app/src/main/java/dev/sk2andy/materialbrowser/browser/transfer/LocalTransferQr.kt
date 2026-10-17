package dev.sk2andy.materialbrowser.browser.transfer

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPairingRules
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [code] as a QR image of [sizePx] square pixels, drawn in [foreground] on [background] (the
 * caller's theme tokens; a scanner needs the dark modules on the light ground).
 */
internal fun localTransferQrBitmap(code: String, sizePx: Int, foreground: Int, background: Int): Bitmap {
    val matrix = localTransferQrMatrix(code, sizePx)
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        if (matrix.get(index % matrix.width, index / matrix.width)) foreground else background
    }
    return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
}

/**
 * The QR modules of [code] scaled to [sizePx]. Error correction M with a one-module quiet zone:
 * the code is short, so the modules stay large on any screen.
 */
internal fun localTransferQrMatrix(code: String, sizePx: Int): BitMatrix = QRCodeWriter().encode(
    code,
    BarcodeFormat.QR_CODE,
    sizePx,
    sizePx,
    mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1),
)

/**
 * Reads camera frames until one holds a Vola transfer code, then reports it once. Only the
 * luminance plane is read and frames stay in memory; nothing is saved or sent anywhere.
 */
internal class LocalTransferQrAnalyzer(private val onCode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private val reported = AtomicBoolean(false)
    private val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))

    override fun analyze(image: ImageProxy) {
        image.use {
            if (reported.get()) return
            val text = decode(image) ?: return
            if (text.startsWith(LocalTransferPairingRules.SCHEME) && reported.compareAndSet(false, true)) onCode(text)
        }
    }

    private fun decode(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer.duplicate().apply { rewind() }
        val rowStride = plane.rowStride
        val luminance = ByteArray(buffer.remaining()).also(buffer::get)
        val source = PlanarYUVLuminanceSource(luminance, rowStride, image.height, 0, 0, image.width, image.height, false)
        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (_: ReaderException) {
            null
        } finally {
            reader.reset()
        }
    }
}
