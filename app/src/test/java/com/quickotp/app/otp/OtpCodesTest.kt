package com.quickotp.app.otp

import com.quickotp.app.data.OtpAccount
import org.junit.Assert.assertEquals
import org.junit.Test

class OtpCodesTest {
    @Test
    fun formatCode_groupsSixAndEight() {
        assertEquals("123 456", OtpCodes.formatCode("123456"))
        assertEquals("1234 5678", OtpCodes.formatCode("12345678"))
        assertEquals("12345", OtpCodes.formatCode("12345"))
    }

    @Test
    fun currentCode_totpMatchesGenerator() {
        val account = OtpAccount(
            label = "RFC",
            secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            type = OtpType.TOTP.name,
            algorithm = OtpAlgorithm.SHA1.name,
            digits = 6,
            periodSeconds = 30,
        )
        assertEquals("287082", OtpCodes.currentCode(account, 59_000))
        assertEquals("287 082", OtpCodes.formatCode(OtpCodes.currentCode(account, 59_000)))
    }

    @Test
    fun currentCode_hotpUsesStoredCounter() {
        val account = OtpAccount(
            label = "HOTP",
            secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            type = OtpType.HOTP.name,
            algorithm = OtpAlgorithm.SHA1.name,
            digits = 6,
            counter = 1,
        )
        assertEquals("287082", OtpCodes.currentCode(account, 0))
    }
}
