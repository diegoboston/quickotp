package com.quickotp.app.otp

import com.quickotp.app.data.toAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpAuthUriTest {
    @Test
    fun parse_googleAuthenticatorExample() {
        val uri = "otpauth://totp/ACME%20Co:john.doe@email.com?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=ACME%20Co&algorithm=SHA1&digits=6&period=30"
        val draft = OtpAuthUri.parse(uri)
        assertEquals(OtpType.TOTP, draft.type)
        assertEquals("HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ", draft.secret)
        assertEquals(OtpAlgorithm.SHA1, draft.algorithm)
        assertEquals(6, draft.digits)
        assertEquals(30, draft.periodSeconds)
        assertEquals("ACME Co", draft.issuer)
        assertEquals("john.doe@email.com", draft.accountName)
        assertEquals("ACME Co (john.doe@email.com)", draft.suggestedLabel())
    }

    @Test
    fun parse_unknownIssuerFromBarcodeApps() {
        val uri = "otpauth://totp/%3CUnknown%3E:%3CUnknown%3E?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&algorithm=SHA1&digits=6&period=30&issuer=%3CUnknown%3E"
        val draft = OtpAuthUri.parse(uri)
        assertEquals("<Unknown>", draft.issuer)
        assertEquals("<Unknown>", draft.accountName)
        assertEquals("", draft.suggestedLabel())
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", draft.secret)
    }

    @Test
    fun parse_hotpWithCounter() {
        val uri = "otpauth://hotp/GitHub:alice?secret=gezdgnbvgy3tqojqgezdgnbvgy3tqojq&counter=9&digits=6"
        val draft = OtpAuthUri.parse(uri)
        assertEquals(OtpType.HOTP, draft.type)
        assertEquals(9L, draft.counter)
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", draft.secret)
        assertEquals("GitHub", draft.issuer)
        assertEquals("alice", draft.accountName)
    }

    @Test
    fun parse_defaultsWhenOptionalParamsMissing() {
        val uri = "otpauth://totp/alice?secret=MFRGGZDFMY"
        val draft = OtpAuthUri.parse(uri)
        assertEquals(OtpAlgorithm.SHA1, draft.algorithm)
        assertEquals(6, draft.digits)
        assertEquals(30, draft.periodSeconds)
        assertEquals(0L, draft.counter)
        assertEquals("alice", draft.accountName)
        assertEquals("", draft.issuer)
    }

    @Test
    fun encode_roundTrip() {
        val original = OtpDraft(
            type = OtpType.TOTP,
            secret = "HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ",
            algorithm = OtpAlgorithm.SHA256,
            digits = 8,
            periodSeconds = 60,
            issuer = "ACME Co",
            accountName = "john doe",
        )
        val encoded = OtpAuthUri.encode(original, "OTP for mysite")
        val parsed = OtpAuthUri.parse(encoded)
        assertEquals(original.type, parsed.type)
        assertEquals(original.secret, parsed.secret)
        assertEquals(original.algorithm, parsed.algorithm)
        assertEquals(original.digits, parsed.digits)
        assertEquals(original.periodSeconds, parsed.periodSeconds)
        assertEquals(original.issuer, parsed.issuer)
        assertEquals("OTP for mysite", parsed.accountName)
        assertTrue(encoded.contains("issuer=ACME%20Co"))
    }

    @Test
    fun encode_omitsUnknownIssuer() {
        val draft = OtpDraft(
            type = OtpType.TOTP,
            secret = "MFRGGZDFMY",
            issuer = "<Unknown>",
            accountName = "<Unknown>",
        )
        val uri = OtpAuthUri.encode(draft, "OTP for mysite")
        val parsed = OtpAuthUri.parse(uri)
        assertEquals("", parsed.issuer)
        assertEquals("OTP for mysite", parsed.accountName)
        assertTrue(!uri.contains("issuer="))
    }

    @Test
    fun parse_rejectsMissingSecret() {
        assertThrows(IllegalArgumentException::class.java) {
            OtpAuthUri.parse("otpauth://totp/alice")
        }
    }

    @Test
    fun parse_rejectsNonOtpAuth() {
        assertThrows(IllegalArgumentException::class.java) {
            OtpAuthUri.parse("https://example.com")
        }
    }

    @Test
    fun account_toUri_usesLabel() {
        val account = OtpDraft(
            type = OtpType.TOTP,
            secret = "MFRGGZDFMY",
            issuer = "GitHub",
            accountName = "alice",
        ).toAccount("OTP for mysite", nowMillis = 1L)
        val parsed = OtpAuthUri.parse(account.toOtpAuthUri())
        assertEquals("OTP for mysite", parsed.accountName)
        assertEquals("GitHub", parsed.issuer)
    }
}
