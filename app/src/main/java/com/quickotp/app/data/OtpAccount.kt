package com.quickotp.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quickotp.app.otp.OtpAlgorithm
import com.quickotp.app.otp.OtpAuthUri
import com.quickotp.app.otp.OtpDraft
import com.quickotp.app.otp.OtpType

enum class EntryKind {
    OTP,
    BARCODE,
}

@Entity(tableName = "otp_accounts")
data class OtpAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val secret: String,
    val type: String = OtpType.TOTP.name,
    val algorithm: String = OtpAlgorithm.SHA1.name,
    val digits: Int = 6,
    val periodSeconds: Int = 30,
    val counter: Long = 0L,
    val issuer: String = "",
    val accountName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'OTP'")
    val kind: String = EntryKind.OTP.name,
    @ColumnInfo(defaultValue = "''")
    val barcodeFormat: String = "",
) {
    val isBarcode: Boolean get() = kind == EntryKind.BARCODE.name
    val otpType: OtpType get() = OtpType.fromName(type)
    val otpAlgorithm: OtpAlgorithm get() = OtpAlgorithm.fromName(algorithm)

    fun toDraft(): OtpDraft {
        check(!isBarcode) { "Stored barcodes are not OTP drafts" }
        return OtpDraft(
            type = otpType,
            secret = secret,
            algorithm = otpAlgorithm,
            digits = digits,
            periodSeconds = periodSeconds,
            counter = counter,
            issuer = issuer,
            accountName = accountName.ifBlank { label },
        )
    }

    fun toOtpAuthUri(): String = OtpAuthUri.encode(toDraft(), label)
}

fun OtpDraft.toAccount(label: String, nowMillis: Long = System.currentTimeMillis()): OtpAccount =
    OtpAccount(
        label = label.trim(),
        secret = secret,
        type = type.name,
        algorithm = algorithm.name,
        digits = digits,
        periodSeconds = periodSeconds,
        counter = counter,
        issuer = issuer,
        accountName = accountName,
        createdAt = nowMillis,
    )
