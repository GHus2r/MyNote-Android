package com.mynote.android.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * 主页「大主题」玻璃卡片（Kyant0/backdrop 真玻璃）。
 *
 * 实现机制（分两层）：
 *   1) backdrop 节点：画主题色渐变 + emoji 圆点（这些内容 = "玻璃透出的内容"）
 *      并用 Modifier.layerBackdrop(backdrop) 拍成快照
 *   2) glass 节点：用 Modifier.drawBackdrop(backdrop, ...) 渲染快照 + effects
 *      （vibrancy 提饱和 / blur 模糊 / lens 折射）→ 看起来就是彩色玻璃
 *
 * 内容（标题 / 篇数 / E D 按钮）放在 glass 节点之上保持可读。
 */
@Composable
fun GlassCategoryCard(
    name: String,
    count: Int,
    colorHex: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = parseColor(colorHex)
    val baseColor = lighten(color, 0.30f)
    val endColor = lighten(color, 0.55f)
    val shape = RoundedCornerShape(16.dp)
    val backdrop = rememberLayerBackdrop { /* 内容由 backdrop 节点画 */ }

    Box(
        Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        // 1) backdrop 节点：主题色渐变 + 彩色光斑 = "被玻璃透出的内容"
        //    光斑必须有清晰的深色边缘，blur 才会糊成柔和色块、lens 才会折射扭曲 → 玻璃感明显
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(baseColor, endColor)))
                .layerBackdrop(backdrop)
        ) {
            // 光斑 1：主题色加深，大号，左上
            Box(
                Modifier
                    .offset(x = (-14).dp, y = (-16).dp)
                    .size(84.dp)
                    .background(darken(baseColor, 0.20f).copy(alpha = 0.50f), CircleShape)
            )
            // 光斑 2：主题色亮化，中号，右下
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 14.dp)
                    .size(64.dp)
                    .background(lighten(baseColor, 0.12f).copy(alpha = 0.60f), CircleShape)
            )
            // 光斑 3：暖色点缀，小号，中部偏右
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 18.dp, y = (-6).dp)
                    .size(40.dp)
                    .background(Color(0xFFFFB74D).copy(alpha = 0.45f), CircleShape)
            )
        }

        // 2) glass 节点：在 backdrop 之上渲染"模糊 + 饱和 + 折射"的快照
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(8f.dp.toPx(), 16f.dp.toPx())
                    }
                )
        )

        // 3) 真实内容（不透明，压在玻璃之上保持可读）
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 14.dp, top = 12.dp, bottom = 10.dp, end = 8.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(36.dp))
                    Text(
                        name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "$count 篇",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 36.dp)
                )
            }

            Row(
                Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionButton("E", Color(0xFF2196F3), onEdit)
                ActionButton("D", Color(0xFFF44336), onDelete)
            }
        }
    }
}

/** 主页「添加大主题」玻璃卡片。 */
@Composable
fun GlassAddCard(onAdd: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val backdrop = rememberLayerBackdrop { }

    Box(
        Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(shape)
            .clickable(onClick = onAdd)
    ) {
        // 1) backdrop 节点：白色 + 灰色渐变作为"被透出的内容"
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFE8E8E8)
                        )
                    )
                )
                .layerBackdrop(backdrop)
        )

        // 2) glass 节点
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                    }
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), shape)
        )

        // 3) 内容
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "+",
                    color = Color(0xFF555555),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "添加大主题",
                    color = Color(0xFF666666),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(26.dp)
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF2196F3)
}

private fun lighten(color: Color, factor: Float): Color = Color(
    red = color.red + (1f - color.red) * factor,
    green = color.green + (1f - color.green) * factor,
    blue = color.blue + (1f - color.blue) * factor
)

private fun darken(color: Color, factor: Float): Color = Color(
    red = color.red * (1f - factor),
    green = color.green * (1f - factor),
    blue = color.blue * (1f - factor)
)
