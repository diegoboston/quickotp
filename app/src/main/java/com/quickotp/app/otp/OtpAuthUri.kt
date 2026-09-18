package com.quickotp.app.otp

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class OtpDraft(
    val type: OtpType,
    val secret: String,
    val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    val digits: Int = 6,
    val periodSeconds: Int = 30,
    val counter: Long = 0L,
    val issuer: String = "",
    val accountName: String = "",
) {
    fun suggestedLabel(): String {
        val issuerClean = displayPart(issuer)
        val accountClean = displayPart(accountName)
        return when {
            issuerClean != null && accountClean != null &&
                !issuerClean.equals(accountClean, ignoreCase = true) ->
                "$issuerClean ($accountClean)"
            issuerClean != null -> issuerClean
            accountClean != null -> accountClean
            else -> ""
        }
    }

    private fun displayPart(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.equals("<Unknown>", ignoreCase = true)) return null
        if (trimmed.equals("Unknown", ignoreCase = true)) return null
        return trimmed
    }
}

object OtpAuthUri {
    fun parse(uri: String): OtpDraft {
        val trimmed = uri.trim()
        require(trimmed.startsWith("otpauth://", ignoreCase = true)) {
            "Not an otpauth URI"
        }
        val withoutScheme = trimmed.substring("otpauth://".length)
        val typeRaw = withoutScheme.substringBefore('/', missingDelimiterValue = withoutScheme)
            .substringBefore('?')
        val type = OtpType.fromName(typeRaw)
        val afterType = withoutScheme.substringAfter('/', missingDelimiterValue = "")
        val labelEncoded = afterType.substringBefore('?')
        val query = afterType.substringAfter('?', missingDelimiterValue = "")
        val params = parseQuery(query)

        val secretRaw = params["secret"] ?: throw IllegalArgumentException("Missing secret")
        val secret = Base32.normalize(secretRaw)
        require(Base32.decode(secret).isNotEmpty()) { "Secret is empty" }

        val digits = params["digits"]?.toIntOrNull() ?: 6
        require(digits in 5..10) { "Digits must be between 5 and 10" }
        val period = params["period"]?.toIntOrNull() ?: 30
        require(period > 0) { "Period must be positive" }
        val counter = params["counter"]?.toLongOrNull() ?: 0L
        require(counter >= 0) { "Counter must be non-negative" }
        val algorithm = OtpAlgorithm.fromName(params["algorithm"])

        val label = decode(labelEncoded)
        val (issuerFromLabel, accountFromLabel) = splitLabel(label)
        val issuer = params["issuer"]?.let(::decode)?.ifBlank { null } ?: issuerFromLabel

        return OtpDraft(
            type = type,
            secret = secret,
            algorithm = algorithm,
            digits = digits,
            periodSeconds = period,
            counter = counter,
            issuer = issuer,
            accountName = accountFromLabel,
        )
    }

    fun encode(draft: OtpDraft, label: String = draft.suggestedLabel()): String {
        val issuerClean = draft.issuer.trim().takeIf { it.isNotEmpty() && !isUnknown(it) }
        val account = label.trim().ifBlank { draft.accountName.trim() }.ifBlank { "Account" }
        val path = if (issuerClean != null) {
            "${encodeComponent(issuerClean)}:${encodeComponent(account)}"
        } else {
            encodeComponent(account)
        }
        val params = mutableListOf("secret=${draft.secret}")
        params += "algorithm=${draft.algorithm.name}"
        params += "digits=${draft.digits}"
        if (draft.type == OtpType.TOTP) {
            params += "period=${draft.periodSeconds}"
        } else {
            params += "counter=${draft.counter}"
        }
        if (issuerClean != null) {
            params += "issuer=${encodeComponent(issuerClean)}"
        }
        return "otpauth://${draft.type.name.lowercase()}/$path?${params.joinToString("&")}"
    }

    private fun splitLabel(label: String): Pair<String, String> {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return "" to ""
        val colon = trimmed.indexOf(':')
        if (colon <= 0) return "" to trimmed
        return trimmed.substring(0, colon).trim() to trimmed.substring(colon + 1).trim()
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        val out = linkedMapOf<String, String>()
        for (part in query.split('&')) {
            if (part.isEmpty()) continue
            val key = decode(part.substringBefore('=')).lowercase()
            val value = decode(part.substringAfter('=', missingDelimiterValue = ""))
            if (key.isNotEmpty()) out[key] = value
        }
        return out
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8.name())

    private fun encodeComponent(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private fun isUnknown(value: String): Boolean =
        value.equals("<Unknown>", ignoreCase = true) || value.equals("Unknown", ignoreCase = true)
}

object OtpInputParser {
    fun parse(raw: String): OtpDraft {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "Enter a secret or otpauth URI" }
        return if (trimmed.startsWith("otpauth://", ignoreCase = true)) {
            OtpAuthUri.parse(trimmed)
        } else {
            val secret = Base32.normalize(trimmed)
            require(Base32.decode(secret).isNotEmpty()) { "Secret is empty" }
            OtpDraft(type = OtpType.TOTP, secret = secret)
        }
    }
}
