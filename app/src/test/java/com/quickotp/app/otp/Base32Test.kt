package com.quickotp.app.otp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Base32Test {
    @Test
    fun encodeDecode_hello() {
        val encoded = Base32.encode("Hello".toByteArray())
        assertEquals("JBSWY3DP", encoded)
        assertEquals("Hello", String(Base32.decode(encoded)))
    }

    @Test
    fun decode_rfc4648Vectors() {
        assertArrayEquals(byteArrayOf(), Base32.decode(""))
        assertEquals("f", String(Base32.decode("MY======")))
        assertEquals("fo", String(Base32.decode("MZXQ====")))
        assertEquals("foo", String(Base32.decode("MZXW6===")))
        assertEquals("foob", String(Base32.decode("MZXW6YQ=")))
        assertEquals("fooba", String(Base32.decode("MZXW6YTB")))
        assertEquals("foobar", String(Base32.decode("MZXW6YTBOI======")))
    }

    @Test
    fun encode_rfc4648Vectors() {
        assertEquals("MY", Base32.encode("f".toByteArray()))
        assertEquals("MY======", Base32.encode("f".toByteArray(), padding = true))
        assertEquals("MZXW6YTBOI", Base32.encode("foobar".toByteArray()))
    }

    @Test
    fun decode_stripsPaddingAndWhitespace() {
        val secret = "GEZDGNBV GY3TQOJQ GEZDGNBV GY3TQOJQ======"
        val decoded = Base32.decode(secret)
        assertArrayEquals("12345678901234567890".toByteArray(), decoded)
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", Base32.normalize(secret))
    }

    @Test
    fun encode_rfc4226Secret() {
        assertEquals(
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            Base32.encode("12345678901234567890".toByteArray()),
        )
    }

    @Test
    fun decode_rejectsInvalidCharacters() {
        assertThrows(IllegalArgumentException::class.java) {
            Base32.decode("ABC1")
        }
    }

    @Test
    fun decode_emptyAfterNormalize_returnsEmpty() {
        assertArrayEquals(byteArrayOf(), Base32.decode(" === "))
    }

    @Test
    fun roundTrip_binary() {
        val original = byteArrayOf(0x00, 0xFF.toByte(), 0x10, 0x80.toByte(), 0x7F)
        val encoded = Base32.encode(original)
        assertArrayEquals(original, Base32.decode(encoded))
    }
}
