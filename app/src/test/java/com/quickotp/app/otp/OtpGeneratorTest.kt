package com.quickotp.app.otp

import org.junit.Assert.assertEquals
import org.junit.Test

class OtpGeneratorTest {
    private val sha1Secret = "12345678901234567890".toByteArray()
    private val sha256Secret = "12345678901234567890123456789012".toByteArray()
    private val sha512Secret = "1234567890123456789012345678901234567890123456789012345678901234".toByteArray()

    @Test
    fun hotp_rfc4226() {
        val expected = listOf(
            0L to "755224",
            1L to "287082",
            2L to "359152",
            3L to "969429",
            4L to "338314",
            5L to "254676",
            6L to "287922",
            7L to "162583",
            8L to "399871",
            9L to "520489",
        )
        for ((counter, code) in expected) {
            assertEquals(
                "counter $counter",
                code,
                OtpGenerator.hotp(sha1Secret, counter, OtpAlgorithm.SHA1, 6),
            )
        }
    }

    @Test
    fun totp_rfc6238_sha1_8digits() {
        val vectors = listOf(
            59L to "94287082",
            1111111109L to "07081804",
            1111111111L to "14050471",
            1234567890L to "89005924",
            2000000000L to "69279037",
            20000000000L to "65353130",
        )
        for ((unixSeconds, code) in vectors) {
            assertEquals(
                "t=$unixSeconds",
                code,
                OtpGenerator.totp(sha1Secret, unixSeconds * 1000, 30, OtpAlgorithm.SHA1, 8),
            )
        }
    }

    @Test
    fun totp_rfc6238_sha256_8digits() {
        val vectors = listOf(
            59L to "46119246",
            1111111109L to "68084774",
            1111111111L to "67062674",
            1234567890L to "91819424",
            2000000000L to "90698825",
            20000000000L to "77737706",
        )
        for ((unixSeconds, code) in vectors) {
            assertEquals(
                "t=$unixSeconds",
                code,
                OtpGenerator.totp(sha256Secret, unixSeconds * 1000, 30, OtpAlgorithm.SHA256, 8),
            )
        }
    }

    @Test
    fun totp_rfc6238_sha512_8digits() {
        val vectors = listOf(
            59L to "90693936",
            1111111109L to "25091201",
            1111111111L to "99943326",
            1234567890L to "93441116",
            2000000000L to "38618901",
            20000000000L to "47863826",
        )
        for ((unixSeconds, code) in vectors) {
            assertEquals(
                "t=$unixSeconds",
                code,
                OtpGenerator.totp(sha512Secret, unixSeconds * 1000, 30, OtpAlgorithm.SHA512, 8),
            )
        }
    }

    @Test
    fun totp_6digits_usesLastSix() {
        assertEquals(
            "287082",
            OtpGenerator.totp(sha1Secret, 59_000, 30, OtpAlgorithm.SHA1, 6),
        )
    }

    @Test
    fun totp_fromBase32Secret() {
        val secret = Base32.decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        assertEquals(
            "287082",
            OtpGenerator.totp(secret, 59_000, 30, OtpAlgorithm.SHA1, 6),
        )
    }
}
