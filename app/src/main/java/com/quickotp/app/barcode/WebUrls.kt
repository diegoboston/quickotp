package com.quickotp.app.barcode

import java.net.URI

object WebUrls {
    /**
     * The trimmed value when the whole string is an absolute http or https URL.
     * Surrounding whitespace is ignored. Anything else, including other schemes, is not a link.
     */
    fun httpUrlOrNull(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (!uri.isAbsolute || uri.rawAuthority.isNullOrEmpty() || uri.host.isNullOrBlank()) {
            return null
        }
        return trimmed
    }
}
