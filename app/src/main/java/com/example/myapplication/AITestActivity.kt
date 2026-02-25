package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.presentation.screen.AITestScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

/**
 * AI测试Activity
 * 用于独立测试ObjectDetector功能
 */
class AITestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AITestScreen()
            }
        }
    }
}
