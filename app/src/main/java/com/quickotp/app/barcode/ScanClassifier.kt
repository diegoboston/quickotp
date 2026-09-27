package com.quickotp.app.barcode

import com.quickotp.app.otp.OtpDraft
import com.quickotp.app.otp.OtpInputParser

sealed interface ClassifiedScan {
    data class Otp(val draft: OtpDraft) : ClassifiedScan
    data class Stored(val value: String, val format: String) : ClassifiedScan
}

object ScanClassifier {
    fun classify(raw: String, format: String): ClassifiedScan {
        require(raw.isNotBlank()) { "Empty barcode" }
        return try {
            ClassifiedScan.Otp(OtpInputParser.parse(raw.trim()))
        } catch (_: Exception) {
            ClassifiedScan.Stored(
                value = raw,
                format = BarcodeCodec.normalizeFormatName(format),
            )
        }
    }
}
