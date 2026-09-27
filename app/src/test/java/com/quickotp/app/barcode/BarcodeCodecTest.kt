package com.quickotp.app.barcode

import com.google.zxing.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeCodecTest {
    @Test
    fun encode_qr_roundTripsArbitraryText() {
        val value = "https://example.com/a?b=1&c=café"
        val encoded = BarcodeCodec.encode(value, "QR_CODE", size = 360)
        assertEquals(BarcodeFormat.QR_CODE, encoded.format)
        assertEquals(value, BarcodeCodec.decode(encoded.matrix))
    }

    @Test
    fun encode_code128_roundTripsAndIsWide() {
        val value = "ABC-123"
        val encoded = BarcodeCodec.encode(value, "CODE_128")
        assertEquals(BarcodeFormat.CODE_128, encoded.format)
        assertTrue(encoded.matrix.width > encoded.matrix.height)
        assertEquals(value, BarcodeCodec.decode(encoded.matrix))
    }

    @Test
    fun encode_invalidEan_fallsBackToQrOfTheSameValue() {
        val value = "not-an-ean"
        val encoded = BarcodeCodec.encode(value, "EAN_13")
        assertEquals(BarcodeFormat.QR_CODE, encoded.format)
        assertEquals(value, BarcodeCodec.decode(encoded.matrix))
    }

    @Test
    fun normalizeFormatName_acceptsScannerAliases() {
        assertEquals("PDF_417", BarcodeCodec.normalizeFormatName("pdf417"))
        assertEquals("CODE_128", BarcodeCodec.normalizeFormatName("code-128"))
        assertEquals("QR_CODE", BarcodeCodec.normalizeFormatName("  "))
    }
}
