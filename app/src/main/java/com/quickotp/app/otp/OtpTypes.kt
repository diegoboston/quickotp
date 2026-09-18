package com.quickotp.app.otp

enum class OtpType {
    TOTP,
    HOTP,
    ;

    companion object {
        fun fromName(raw: String): OtpType = when (raw.trim().uppercase()) {
            "TOTP" -> TOTP
            "HOTP" -> HOTP
            else -> throw IllegalArgumentException("Unsupported OTP type: $raw")
        }
    }
}

enum class OtpAlgorithm(val macName: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512"),
    ;

    companion object {
        fun fromName(raw: String?): OtpAlgorithm {
            val n = raw?.uppercase()?.replace("-", "")?.replace("_", "")?.trim().orEmpty()
            if (n.isEmpty()) return SHA1
            return when (n) {
                "SHA1", "SHA1HMAC", "HMACSHA1" -> SHA1
                "SHA256", "HMACSHA256" -> SHA256
                "SHA512", "HMACSHA512" -> SHA512
                else -> throw IllegalArgumentException("Unsupported algorithm: $raw")
            }
        }
    }
}
