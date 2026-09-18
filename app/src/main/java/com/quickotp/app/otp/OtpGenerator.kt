package com.quickotp.app.otp

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object OtpGenerator {
    fun totp(
        secret: ByteArray,
        timeMillis: Long,
        periodSeconds: Int,
        algorithm: OtpAlgorithm,
        digits: Int,
    ): String {
        require(periodSeconds > 0) { "Period must be positive" }
        val counter = timeMillis.coerceAtLeast(0L) / 1000L / periodSeconds
        return generate(secret, counter, algorithm, digits)
    }

    fun hotp(
        secret: ByteArray,
        counter: Long,
        algorithm: OtpAlgorithm,
        digits: Int,
    ): String = generate(secret, counter, algorithm, digits)

    fun generate(
        secret: ByteArray,
        counter: Long,
        algorithm: OtpAlgorithm,
        digits: Int,
    ): String {
        require(secret.isNotEmpty()) { "Secret is empty" }
        require(digits in 5..10) { "Digits must be between 5 and 10" }
        require(counter >= 0) { "Counter must be non-negative" }
        val hash = hmac(algorithm, secret, counterBytes(counter))
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val modulus = 10.0.pow(digits).toLong()
        return (binary.toLong() % modulus).toString().padStart(digits, '0')
    }

    private fun hmac(algorithm: OtpAlgorithm, secret: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(algorithm.macName)
        mac.init(SecretKeySpec(secret, algorithm.macName))
        return mac.doFinal(data)
    }

    private fun counterBytes(counter: Long): ByteArray {
        val bytes = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) {
            bytes[i] = (value and 0xFF).toByte()
            value = value ushr 8
        }
        return bytes
    }
}
