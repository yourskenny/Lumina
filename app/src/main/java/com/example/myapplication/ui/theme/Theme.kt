package com.example.myapplication.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

// 高对比度深色主题 - 用于无障碍模式
private val HighContrastDarkColorScheme = darkColorScheme(
    primary = HighContrastWhite,
    onPrimary = HighContrastBlack,
    secondary = HighContrastYellow,
    onSecondary = HighContrastBlack,
    tertiary = HighContrastBlue,
    onTertiary = HighContrastWhite,
    background = HighContrastBlack,
    onBackground = HighContrastWhite,
    surface = HighContrastBlack,
    onSurface = HighContrastWhite,
    error = HighContrastRed,
    onError = HighContrastWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // 检测系统高对比度设置
    // 注意: isHighTextContrastEnabled 只在 Android 14 (API 34) 及以上可用
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val isHighContrastEnabled = try {
        if (Build.VERSION.SDK_INT >= 34) {
            // 使用反射访问 API 34+ 的方法
            val method = AccessibilityManager::class.java.getMethod("isHighTextContrastEnabled")
            method.invoke(accessibilityManager) as? Boolean ?: false
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }

    val colorScheme = when {
        // 高对比度模式优先
        isHighContrastEnabled -> HighContrastDarkColorScheme

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}