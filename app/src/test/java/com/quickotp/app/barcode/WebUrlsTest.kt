package com.quickotp.app.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebUrlsTest {
    @Test
    fun httpUrl_acceptsHttpAndHttps() {
        assertEquals("https://example.com/door", WebUrls.httpUrlOrNull("https://example.com/door"))
        assertEquals(
            "http://example.com/path?q=1#door",
            WebUrls.httpUrlOrNull("http://example.com/path?q=1#door"),
        )
    }

    @Test
    fun httpUrl_trimsSurroundingWhitespace() {
        assertEquals(
            "https://example.com/door",
            WebUrls.httpUrlOrNull("  https://example.com/door\n"),
        )
    }

    @Test
    fun httpUrl_acceptsUppercaseScheme() {
        assertEquals("HTTP://Example.COM/A", WebUrls.httpUrlOrNull("HTTP://Example.COM/A"))
    }

    @Test
    fun httpUrl_rejectsNonWebText() {
        assertNull(WebUrls.httpUrlOrNull("lobby door"))
        assertNull(WebUrls.httpUrlOrNull("otpauth://totp/alice"))
        assertNull(WebUrls.httpUrlOrNull("mailto:ada@example.com"))
        assertNull(WebUrls.httpUrlOrNull("ftp://example.com/file"))
        assertNull(WebUrls.httpUrlOrNull("example.com"))
        assertNull(WebUrls.httpUrlOrNull("https://"))
        assertNull(WebUrls.httpUrlOrNull("https://example.com/a b"))
        assertNull(WebUrls.httpUrlOrNull("  "))
    }
}
