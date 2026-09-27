package com.quickotp.app.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodec {
    fun encode(content: String, size: Int = 512): BitMatrix {
        require(content.isNotEmpty()) { "QR content is empty" }
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 2,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        )
        return QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    }

    fun decode(matrix: BitMatrix): String {
        val source = BitMatrixLuminanceSource(matrix)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(
            DecodeHintType.CHARACTER_SET to "UTF-8",
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        )
        return MultiFormatReader().decode(bitmap, hints).text
    }
}

internal class BitMatrixLuminanceSource(
    private val matrix: BitMatrix,
) : LuminanceSource(matrix.width, matrix.height) {
    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val out = if (row == null || row.size < width) ByteArray(width) else row
        for (x in 0 until width) {
            out[x] = if (matrix.get(x, y)) 0 else 0xFF.toByte()
        }
        return out
    }

    override fun getMatrix(): ByteArray {
        val out = ByteArray(width * height)
        var i = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                out[i++] = if (matrix.get(x, y)) 0 else 0xFF.toByte()
            }
        }
        return out
    }
}
