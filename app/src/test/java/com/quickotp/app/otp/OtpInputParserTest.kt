package com.quickotp.app.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OtpInputParserTest {
    @Test
    fun parse_rawBase32_defaultsToTotpSha1() {
        val draft = OtpInputParser.parse(" gezdgnbv gy3tqojq gezdgnbv gy3tqojq ")
        assertEquals(OtpType.TOTP, draft.type)
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", draft.secret)
        assertEquals(OtpAlgorithm.SHA1, draft.algorithm)
        assertEquals(6, draft.digits)
        assertEquals(30, draft.periodSeconds)
        assertEquals("", draft.suggestedLabel())
    }

    @Test
    fun parse_otpauthUri() {
        val draft = OtpInputParser.parse(
            "otpauth://totp/GitHub:alice?secret=MFRGGZDFMY&issuer=GitHub",
        )
        assertEquals("GitHub", draft.issuer)
        assertEquals("alice", draft.accountName)
        assertEquals("MFRGGZDFMY", draft.secret)
    }

    @Test
    fun parse_rejectsEmpty() {
        assertThrows(IllegalArgumentException::class.java) {
            OtpInputParser.parse("   ")
        }
    }

    @Test
    fun parse_rejectsInvalidBase32() {
        assertThrows(IllegalArgumentException::class.java) {
            OtpInputParser.parse("not a secret!!!")
        }
    }
}
