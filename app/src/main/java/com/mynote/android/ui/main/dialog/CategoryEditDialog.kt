package com.mynote.android.ui.main.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynote.android.ui.glass.GlassDialogFragment
import com.mynote.android.ui.glass.GlassPanel

/**
 * 大主题/子主题编辑弹窗（共用）— Haze 玻璃材质版
 */
class CategoryEditDialog(
    private val title: String,
    private val initialName: String,
    private val initialColor: String,
    private val onSave: (name: String, color: String) -> Unit
) : GlassDialogFragment() {

    // 30 色，按彩虹/色相顺序排列
    private val COLORS = arrayOf(
        "#000000", "#FFFFFF", "#9E9E9E", "#607D8B", "#795548",
        "#F44336", "#E91E63", "#FF5252", "#D32F2F", "#B71C1C",
        "#FF5722", "#FF9800", "#FFC107", "#FFEB3B", "#FFD54F",
        "#4CAF50", "#8BC34A", "#00E676", "#1B5E20", "#A5D6A7",
        "#009688", "#00BCD4", "#2196F3", "#03A9F4", "#3F51B5",
        "#9C27B0", "#673AB7", "#E040FB", "#D500F9", "#B39DDB"
    )

    @Composable
    override fun Content() {
        var name by remember { mutableStateOf(initialName) }
        var selected by remember { mutableStateOf(initialColor) }

        GlassPanel {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF212121)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("主题名称") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))

            // 颜色选择器：30 个色球，5 列网格（与旧版一致）
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(COLORS) { hex ->
                    ColorDot(
                        hex = hex,
                        selected = selected == hex,
                        onClick = { selected = hex }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { dismiss() }) {
                    Text("取消", color = Color(0xFF757575))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotEmpty()) {
                            onSave(trimmed, selected)
                            dismiss()
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("保存")
                }
            }
        }
    }

    @Composable
    private fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
        val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
            .getOrDefault(Color.Gray)
        // 白色圆点加浅灰边框，避免在浅背景上看不见（与旧版一致）
        val baseStroke = if (hex == "#FFFFFF") Color(0xFFBDBDBD) else Color.Transparent
        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer(
                    scaleX = if (selected) 1.2f else 1f,
                    scaleY = if (selected) 1.2f else 1f
                )
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) Color.White else baseStroke,
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
        )
    }
}
