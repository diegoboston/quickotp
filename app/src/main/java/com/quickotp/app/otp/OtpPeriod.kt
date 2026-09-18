package com.quickotp.app.otp

object OtpPeriod {
    const val URGENT_WINDOW_MILLIS = 10_000L

    fun remainingMillis(timeMillis: Long, periodSeconds: Int): Long {
        require(periodSeconds > 0) { "Period must be positive" }
        val periodMillis = periodSeconds * 1000L
        val elapsed = timeMillis.coerceAtLeast(0L) % periodMillis
        return periodMillis - elapsed
    }

    fun progress(timeMillis: Long, periodSeconds: Int): Float {
        val periodMillis = periodSeconds * 1000L
        return remainingMillis(timeMillis, periodSeconds).toFloat() / periodMillis.toFloat()
    }

    fun isUrgent(
        timeMillis: Long,
        periodSeconds: Int,
        urgentWindowMillis: Long = URGENT_WINDOW_MILLIS,
    ): Boolean = remainingMillis(timeMillis, periodSeconds) <= urgentWindowMillis
}
