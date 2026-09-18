package com.quickotp.app

import android.app.Application
import com.quickotp.app.data.AppDatabase
import com.quickotp.app.data.OtpRepository

class QuickOtpApp : Application() {
    lateinit var otpRepository: OtpRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        otpRepository = OtpRepository(db.otpAccountDao())
    }

    companion object {
        fun from(app: Application): QuickOtpApp = app as QuickOtpApp
    }
}
