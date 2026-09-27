package com.quickotp.app.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

data class EncodedBarcode(
    val matrix: BitMatrix,
    val format: BarcodeFormat,
)

object BarcodeCodec {
    fun encode(content: String, formatName: String, size: Int = 512): EncodedBarcode {
        require(content.isNotEmpty()) { "Barcode content is empty" }
        val requested = zxingFormat(formatName)
        if (requested == BarcodeFormat.QR_CODE) {
            return EncodedBarcode(write(content, requested, size, size), requested)
        }
        val (width, height) = dimensions(requested, size)
        return try {
            EncodedBarcode(write(content, requested, width, height), requested)
        } catch (_: Exception) {
            EncodedBarcode(QrCodec.encode(content, size), BarcodeFormat.QR_CODE)
        }
    }

    fun decode(matrix: BitMatrix): String {
        val source = BitMatrixLuminanceSource(matrix)
        val hints = mapOf(
            DecodeHintType.CHARACTER_SET to "UTF-8",
            DecodeHintType.TRY_HARDER to true,
        )
        val reader = MultiFormatReader()
        return try {
            reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), hints).text
        } catch (_: Exception) {
            reader.reset()
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        }
    }

    fun zxingFormat(formatName: String): BarcodeFormat {
        val normalized = normalizeFormatName(formatName)
        return runCatching { BarcodeFormat.valueOf(normalized) }.getOrDefault(BarcodeFormat.QR_CODE)
    }

    internal fun normalizeFormatName(formatName: String): String {
        val n = formatName.trim().uppercase().replace('-', '_').replace(' ', '_')
        return when (n) {
            "PDF417" -> "PDF_417"
            "DATAMATRIX" -> "DATA_MATRIX"
            "CODE128" -> "CODE_128"
            "CODE39" -> "CODE_39"
            "CODE93" -> "CODE_93"
            "EAN13" -> "EAN_13"
            "EAN8" -> "EAN_8"
            "UPCA" -> "UPC_A"
            "UPCE" -> "UPC_E"
            "" -> "QR_CODE"
            else -> n
        }
    }

    private fun dimensions(format: BarcodeFormat, size: Int): Pair<Int, Int> = when (format) {
        BarcodeFormat.QR_CODE,
        BarcodeFormat.AZTEC,
        BarcodeFormat.DATA_MATRIX,
        -> size to size
        BarcodeFormat.PDF_417 -> size to (size / 3).coerceAtLeast(120)
        else -> size to (size / 4).coerceAtLeast(120)
    }

    private fun write(content: String, format: BarcodeFormat, width: Int, height: Int): BitMatrix {
        val hints = mutableMapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to 2)
        if (format == BarcodeFormat.QR_CODE ||
            format == BarcodeFormat.AZTEC ||
            format == BarcodeFormat.DATA_MATRIX ||
            format == BarcodeFormat.PDF_417
        ) {
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        }
        if (format == BarcodeFormat.QR_CODE) {
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        }
        return MultiFormatWriter().encode(content, format, width, height, hints)
    }
}
