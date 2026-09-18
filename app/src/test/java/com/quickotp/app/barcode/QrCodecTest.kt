package com.quickotp.app.barcode

import com.quickotp.app.otp.OtpAlgorithm
import com.quickotp.app.otp.OtpAuthUri
import com.quickotp.app.otp.OtpDraft
import com.quickotp.app.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodecTest {
    @Test
    fun encodeDecode_roundTripOtpAuthUri() {
        val draft = OtpDraft(
            type = OtpType.TOTP,
            secret = "HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ",
            algorithm = OtpAlgorithm.SHA1,
            digits = 6,
            periodSeconds = 30,
            issuer = "ACME Co",
            accountName = "john.doe@email.com",
        )
        val uri = OtpAuthUri.encode(draft, "OTP for mysite")
        val matrix = QrCodec.encode(uri, size = 320)
        assertTrue(matrix.width >= 100)
        assertTrue(matrix.height >= 100)
        val decoded = QrCodec.decode(matrix)
        assertEquals(uri, decoded)
        val parsed = OtpAuthUri.parse(decoded)
        assertEquals(draft.secret, parsed.secret)
        assertEquals("OTP for mysite", parsed.accountName)
    }

    @Test
    fun encode_containsBothDarkAndLightModules() {
        val matrix = QrCodec.encode("otpauth://totp/Test?secret=MFRGGZDFMY")
        var dark = 0
        var light = 0
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) dark++ else light++
            }
        }
        assertTrue(dark > 0)
        assertTrue(light > 0)
    }
}
