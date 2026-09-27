package com.quickotp.app.barcode

import com.quickotp.app.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanClassifierTest {
    @Test
    fun classify_otpauth_isOtp() {
        val result = ScanClassifier.classify(
            "otpauth://totp/GitHub:alice?secret=MFRGGZDFMY&issuer=GitHub",
            "QR_CODE",
        )
        assertTrue(result is ClassifiedScan.Otp)
        val otp = result as ClassifiedScan.Otp
        assertEquals(OtpType.TOTP, otp.draft.type)
        assertEquals("MFRGGZDFMY", otp.draft.secret)
    }

    @Test
    fun classify_base32Secret_isOtp() {
        val result = ScanClassifier.classify("MFRGGZDFMY", "QR_CODE")
        assertTrue(result is ClassifiedScan.Otp)
    }

    @Test
    fun classify_otherBarcode_storesValueAsScanned() {
        val raw = "  https://example.com/ticket/42  "
        val result = ScanClassifier.classify(raw, "code-128")
        assertTrue(result is ClassifiedScan.Stored)
        val stored = result as ClassifiedScan.Stored
        assertEquals(raw, stored.value)
        assertEquals("CODE_128", stored.format)
    }

    @Test
    fun classify_malformedOtpAuth_isStoredVerbatim() {
        val raw = "otpauth://totp/alice"
        val result = ScanClassifier.classify(raw, "QR_CODE")
        assertTrue(result is ClassifiedScan.Stored)
        assertEquals(raw, (result as ClassifiedScan.Stored).value)
    }

    @Test
    fun classify_blank_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ScanClassifier.classify("   ", "QR_CODE")
        }
    }
}
