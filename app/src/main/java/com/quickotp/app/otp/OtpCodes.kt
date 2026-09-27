package com.quickotp.app.otp

import com.quickotp.app.data.OtpAccount

object OtpCodes {
    fun currentCode(account: OtpAccount, timeMillis: Long): String {
        require(!account.isBarcode) { "Stored barcodes have no OTP code" }
        val secret = Base32.decode(account.secret)
        return when (account.otpType) {
            OtpType.TOTP -> OtpGenerator.totp(
                secret = secret,
                timeMillis = timeMillis,
                periodSeconds = account.periodSeconds,
                algorithm = account.otpAlgorithm,
                digits = account.digits,
            )
            OtpType.HOTP -> OtpGenerator.hotp(
                secret = secret,
                counter = account.counter,
                algorithm = account.otpAlgorithm,
                digits = account.digits,
            )
        }
    }

    fun formatCode(code: String): String = when (code.length) {
        6 -> "${code.substring(0, 3)} ${code.substring(3)}"
        8 -> "${code.substring(0, 4)} ${code.substring(4)}"
        else -> code
    }
}
