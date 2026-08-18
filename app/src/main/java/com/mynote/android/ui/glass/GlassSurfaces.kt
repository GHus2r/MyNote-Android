package com.mynote.android.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * 顶栏真玻璃背景（Kyant0/backdrop，blur + vibrancy + lens）。
 *
 * 玻璃透出「蓝紫渐变 + 彩色光斑」，有清晰的圆形边缘，lens 折射会明显扭曲光斑边缘。
 * 用作顶部栏的背景层，顶栏按钮（View）浮在其上。
 */
@Composable
fun GlassBarBackground(modifier: Modifier = Modifier) {
    val backdrop = rememberLayerBackdrop { }

    Box(modifier.fillMaxSize()) {
        // 1) backdrop 节点：蓝紫渐变 + 彩色光斑 = 被玻璃透出的内容
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF667EEA), Color(0xFF764BA2), Color(0xFF5B8DEF))
                    )
                )
                .layerBackdrop(backdrop)
        ) {
            // 光斑 1：左侧暖色
            Box(
                Modifier
                    .offset(x = (-16).dp, y = (-18).dp)
                    .size(72.dp)
                    .background(Color(0xFFFFB74D).copy(alpha = 0.55f), CircleShape)
            )
            // 光斑 2：右侧青色
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 24.dp, y = 6.dp)
                    .size(60.dp)
                    .background(Color(0xFF4DD0E1).copy(alpha = 0.50f), CircleShape)
            )
            // 光斑 3：中部紫红
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(x = 60.dp, y = (-10).dp)
                    .size(46.dp)
                    .background(Color(0xFFEC407A).copy(alpha = 0.45f), CircleShape)
            )
        }

        // 2) glass 节点：渲染快照 + 模糊/饱和/折射
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(6f.dp.toPx(), 14f.dp.toPx())
                    }
                )
        )
    }
}

/**
 * 列表卡片玻璃背景（浅色真玻璃，透淡彩光斑，深色文字压在玻璃上保持可读）。
 * 用于子主题/笔记列表卡片。
 */
@Composable
fun GlassCardBackground(modifier: Modifier = Modifier, tintHex: String = "#4A90D9") {
    val tint = parseColor(tintHex)
    val backdrop = rememberLayerBackdrop { }
    val shape = RoundedCornerShape(14.dp)

    Box(modifier.clip(shape)) {
        // 1) backdrop 节点：浅色渐变 + 淡彩光斑
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F4FB))))
                .layerBackdrop(backdrop)
        ) {
            // 左上淡彩光斑
            Box(
                Modifier
                    .offset(x = (-10).dp, y = (-12).dp)
                    .size(56.dp)
                    .background(tint.copy(alpha = 0.20f), CircleShape)
            )
            // 右下暖色光斑
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 10.dp)
                    .size(44.dp)
                    .background(Color(0xFFFFB74D).copy(alpha = 0.18f), CircleShape)
            )
        }
        // 2) glass 节点
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(3f.dp.toPx())
                    }
                )
        )
    }
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF4A90D9)
}

/**
 * 编辑页「更多面板 / 底部栏」真玻璃背景（Kyant0/backdrop）。
 *
 * 浅色渐变 + 淡彩光斑，blur/lens/vibrancy 后形成磨砂玻璃质感；
 * 内容（按钮/图标）直接浮在玻璃上。圆角可分别控制顶部/底部。
 */
@Composable
fun EditGlassPanel(
    modifier: Modifier = Modifier,
    roundedTop: Boolean = false,
    roundedBottom: Boolean = false
) {
    val backdrop = rememberLayerBackdrop { }
    val shape = RoundedCornerShape(
        topStart = if (roundedTop) 20.dp else 0.dp,
        topEnd = if (roundedTop) 20.dp else 0.dp,
        bottomStart = if (roundedBottom) 20.dp else 0.dp,
        bottomEnd = if (roundedBottom) 20.dp else 0.dp
    )

    Box(modifier.fillMaxSize().clip(shape)) {
        // 1) 被模糊的底色：浅紫白渐变 + 淡彩光斑
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF8F6FF), Color(0xFFE8E4F6), Color(0xFFDED9EF))
                    )
                )
                .layerBackdrop(backdrop)
        ) {
            // 左侧淡紫光斑
            Box(
                Modifier
                    .offset(x = (-12).dp, y = (-10).dp)
                    .size(70.dp)
                    .background(Color(0xFF9575CD).copy(alpha = 0.28f), CircleShape)
            )
            // 右侧暖色光斑
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 18.dp, y = 8.dp)
                    .size(58.dp)
                    .background(Color(0xFFFFCC80).copy(alpha = 0.30f), CircleShape)
            )
            // 中部青蓝光斑
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 120.dp, y = (-6).dp)
                    .size(44.dp)
                    .background(Color(0xFF80DEEA).copy(alpha = 0.22f), CircleShape)
            )
        }

        // 2) 玻璃层
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(5f.dp.toPx())
                        lens(5f.dp.toPx(), 12f.dp.toPx())
                    }
                )
        )
    }
}
