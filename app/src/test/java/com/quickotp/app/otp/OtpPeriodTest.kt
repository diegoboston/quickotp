package com.quickotp.app.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpPeriodTest {
    @Test
    fun remainingMillis_atPeriodStart() {
        assertEquals(30_000L, OtpPeriod.remainingMillis(0, 30))
        assertEquals(30_000L, OtpPeriod.remainingMillis(30_000, 30))
        assertEquals(30_000L, OtpPeriod.remainingMillis(60_000, 30))
    }

    @Test
    fun remainingMillis_midPeriod() {
        assertEquals(20_000L, OtpPeriod.remainingMillis(10_000, 30))
        assertEquals(1L, OtpPeriod.remainingMillis(29_999, 30))
    }

    @Test
    fun progress_drainsFromOneToZero() {
        assertEquals(1.0f, OtpPeriod.progress(0, 30), 0.0001f)
        assertEquals(0.5f, OtpPeriod.progress(15_000, 30), 0.0001f)
        assertTrue(OtpPeriod.progress(29_999, 30) < 0.01f)
    }

    @Test
    fun isUrgent_lastTenSeconds() {
        assertFalse(OtpPeriod.isUrgent(0, 30))
        assertFalse(OtpPeriod.isUrgent(19_999, 30))
        assertTrue(OtpPeriod.isUrgent(20_000, 30))
        assertTrue(OtpPeriod.isUrgent(29_999, 30))
    }

    @Test
    fun remaining_customPeriod() {
        assertEquals(15_000L, OtpPeriod.remainingMillis(45_000, 60))
    }
}
