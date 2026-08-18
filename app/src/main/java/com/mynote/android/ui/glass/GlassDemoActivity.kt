package com.mynote.android.ui.glass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * Liquid Glass（Kyant0/backdrop）透明真玻璃示例页。
 *
 * 通过 adb 直接启动：
 *   adb shell am start -n com.mynote.android/.ui.glass.GlassDemoActivity
 *
 * 透明真玻璃三要素：
 *  - vibrancy() 增强饱和度
 *  - blur(4dp)  背景模糊
 *  - lens(...)  折射（需 Android 13+，shape 必须是 RoundedCornerShape 等 CornerBasedShape）
 *  ⚠️ 不要传 onDrawSurface（默认空 lambda）：传了白罩会产生大面积白底色块，破坏透明感。
 *
 * 系统限制：blur/vibrancy 需 Android 12+，lens 需 Android 13+，低版本下效果静默失效（不崩溃），
 * 玻璃组件会退化成"完全透明"（因为省略了白罩）。建议在 Android 13+ 真机查看效果。
 */
class GlassDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GlassDemoScreen()
            }
        }
    }
}

/** 背景渐变（蓝紫色调）。玻璃效果要求背景必须是彩色/有内容，纯白背景 blur 后会糊成一片。 */
private val BackgroundGradient = listOf(
    Color(0xFF7F7FD5),
    Color(0xFF86A8E7),
    Color(0xFF91EAE4)
)

@Composable
fun GlassDemoScreen() {
    Box(Modifier.fillMaxSize()) {
        // 1. 创建 LayerBackdrop（⚠️ 必须放在 Box 内，与 layerBackdrop/drawBackdrop 同一坐标空间，
        //    否则玻璃组件取不到背景快照，会渲染成空的/透明，等于没效果）。
        val backdrop = rememberLayerBackdrop {
            drawRect(Brush.linearGradient(BackgroundGradient))
            drawContent()
        }

        // 2. 背景内容（彩色光斑）—— 通过 layerBackdrop 被快照进 backdrop，成为玻璃透出的内容。
        BackgroundLayer(Modifier.fillMaxSize().layerBackdrop(backdrop))

        // 3. 玻璃组件（覆盖在背景之上，通过 drawBackdrop 取背景 + 效果）。
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))
            GlassCard(backdrop)
            Spacer(Modifier.height(32.dp))
            GlassButton(backdrop)
            Spacer(Modifier.weight(1f))
            GlassBottomBar(backdrop, Modifier.padding(bottom = 24.dp))
        }
    }
}

/** 彩色光斑背景：让玻璃有丰富内容可透，blur/lens 效果才明显。 */
@Composable
private fun BackgroundLayer(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0xFFFF8A65), radius = w * 0.38f, center = Offset(w * 0.12f, h * 0.18f))
        drawCircle(Color(0xFF4FC3F7), radius = w * 0.32f, center = Offset(w * 0.88f, h * 0.28f))
        drawCircle(Color(0xFFBA68C8), radius = w * 0.34f, center = Offset(w * 0.5f, h * 0.58f))
        drawCircle(Color(0xFF81C784), radius = w * 0.30f, center = Offset(w * 0.22f, h * 0.86f))
        drawCircle(Color(0xFFFFD54F), radius = w * 0.26f, center = Offset(w * 0.82f, h * 0.82f))
    }
}

/** 玻璃卡片：模糊 + 折射（透明真玻璃，无白罩）。 */
@Composable
private fun GlassCard(backdrop: Backdrop, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(24.dp) },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                }
            )
            .padding(24.dp)
    ) {
        Text("液态玻璃卡片", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "模糊 + 折射\n背景透过玻璃清晰可见",
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 14.sp
        )
    }
}

/** 玻璃按钮。 */
@Composable
private fun GlassButton(backdrop: Backdrop, modifier: Modifier = Modifier) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(16.dp) },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(8f.dp.toPx(), 16f.dp.toPx())
                }
            )
            .clickable { /* 示例交互占位 */ }
            .padding(horizontal = 32.dp, vertical = 14.dp)
    ) {
        Text("玻璃按钮", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/** 玻璃底部导航栏：4 个 tab。 */
@Composable
private fun GlassBottomBar(backdrop: Backdrop, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(28.dp) },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                }
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("首页", "笔记", "患者", "我的").forEachIndexed { index, label ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(if (index == 0) 12.dp else 10.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(2f.dp.toPx())
                            }
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    color = if (index == 0) Color.White else Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
