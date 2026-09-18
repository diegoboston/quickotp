package com.quickotp.app.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OtpTypesTest {
    @Test
    fun algorithm_aliases() {
        assertEquals(OtpAlgorithm.SHA1, OtpAlgorithm.fromName(null))
        assertEquals(OtpAlgorithm.SHA1, OtpAlgorithm.fromName("sha-1"))
        assertEquals(OtpAlgorithm.SHA256, OtpAlgorithm.fromName("SHA-256"))
        assertEquals(OtpAlgorithm.SHA512, OtpAlgorithm.fromName("hmacsha512"))
    }

    @Test
    fun algorithm_rejectsUnknown() {
        assertThrows(IllegalArgumentException::class.java) {
            OtpAlgorithm.fromName("MD5")
        }
    }

    @Test
    fun type_fromName() {
        assertEquals(OtpType.TOTP, OtpType.fromName("totp"))
        assertEquals(OtpType.HOTP, OtpType.fromName("HOTP"))
    }
}
