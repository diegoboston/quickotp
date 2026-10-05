package com.quickotp.app

import android.app.Application
import com.quickotp.app.data.AppDatabase
import com.quickotp.app.data.OtpRepository

class QuickOtpApp : Application() {
    lateinit var otpRepository: OtpRepository
        private set

    override fun onCreate() {
        super.onCreate()
        otpRepository = OtpRepository(AppDatabase.get(this))
    }

    companion object {
        fun from(app: Application): QuickOtpApp = app as QuickOtpApp
    }
}
