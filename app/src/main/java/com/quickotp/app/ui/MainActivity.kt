package com.quickotp.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.quickotp.app.ui.screens.HomeScreen
import com.quickotp.app.ui.theme.QuickOtpTheme

class MainActivity : ComponentActivity() {
    private val viewModel: OtpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeOtpAuthIntent(intent)
        setContent {
            QuickOtpTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeOtpAuthIntent(intent)
    }

    private fun consumeOtpAuthIntent(intent: Intent?) {
        val uri = intent?.data?.toString() ?: return
        viewModel.tryAcceptInput(uri)
    }
}
