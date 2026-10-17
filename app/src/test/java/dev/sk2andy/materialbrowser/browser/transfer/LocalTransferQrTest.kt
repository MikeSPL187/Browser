package dev.sk2andy.materialbrowser.browser.transfer

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPairing
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPairingRules
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalTransferQrTest {
    @Test
    fun aTransferCodeReadsBackFromItsQrImage() {
        val pairing = LocalTransferPairing(
            host = "192.168.1.20",
            port = 48_123,
            sessionId = ByteArray(16) { it.toByte() },
            key = ByteArray(32) { (it * 7).toByte() },
            expiresAtEpochSeconds = 1_800_000_120,
        )
        val code = LocalTransferPairingRules.format(pairing)
        val matrix = localTransferQrMatrix(code, 400)
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            if (matrix.get(index % matrix.width, index / matrix.width)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }

        val read = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels))))

        assertEquals(code, read.text)
        assertEquals(400, matrix.width)
    }
}
