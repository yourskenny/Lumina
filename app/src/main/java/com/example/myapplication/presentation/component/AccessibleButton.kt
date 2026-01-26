package com.example.myapplication.presentation.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 无障碍按钮组件
 * 特点:
 * - 最小尺寸72dp × 72dp,方便触摸
 * - 大字体24sp,清晰易读
 * - 完整的语义标签,支持TalkBack
 * - 高对比度颜色
 */
@Composable
fun AccessibleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescriptionText: String = text
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 72.dp, minHeight = 72.dp)
            .semantics {
                contentDescription = contentDescriptionText
            },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = text,
            fontSize = 24.sp, // 大字体
            style = MaterialTheme.typography.titleLarge
        )
    }
}
