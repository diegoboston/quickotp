package com.quickotp.app.otp

/**
 * RFC 4648 Base32 (A–Z, 2–7). Padding and whitespace are ignored on decode.
 * otpauth secrets are typically stored without padding.
 */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun normalize(input: String): String = buildString(input.length) {
        for (c in input) {
            when (c) {
                ' ', '\t', '\n', '\r', '-', '=' -> Unit
                else -> append(c.uppercaseChar())
            }
        }
    }

    fun decode(input: String): ByteArray {
        val encoded = normalize(input)
        if (encoded.isEmpty()) return ByteArray(0)
        val out = ArrayList<Byte>(encoded.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        for (c in encoded) {
            val value = ALPHABET.indexOf(c)
            require(value >= 0) { "Invalid Base32 character: $c" }
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }

    fun encode(data: ByteArray, padding: Boolean = false): String {
        if (data.isEmpty()) return ""
        val sb = StringBuilder((data.size * 8 + 4) / 5)
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                sb.append(ALPHABET[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) {
            sb.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        if (padding) {
            while (sb.length % 8 != 0) sb.append('=')
        }
        return sb.toString()
    }
}
