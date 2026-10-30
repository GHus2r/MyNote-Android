package com.mynote.android.ui.glass

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * 全局共享 HazeState：
 * - 背景（ParentGridScreen / SubCategoryScreen 的列表）用 [dev.chrisbanes.haze.hazeSource] 注册为模糊源
 * - 玻璃对话框用 [GlassPanel] 的 hazeEffect 跨窗口取景模糊
 *
 * Haze 按 source 在屏幕上的位置对齐，Dialog 是独立 window 也能正常取景（官方 DialogSample 模式）。
 */
object AppHaze {
    val state: HazeState = HazeState()
}

/**
 * 玻璃面板（对话框内容容器）。
 *
 * 官方 Dialog 模式要点：Dialog 里不能用 haze tint（tint 会显示成蒙在背景上的 scrim），
 * 改用半透明底色 + hazeEffect(HazeMaterials.regular()) 真模糊。
 *
 * **鸿蒙适配**：Haze 基于 RenderEffect，在鸿蒙上不生效。此时若仍用 alpha 0.28 的
 * 半透明底，弹窗内容会与背后页面重叠、无法阅读。因此按 [GlassCompat.isHazeSupported]
 * 二选一：
 * - 支持模糊 → 低 alpha 半透明底 + hazeEffect（保留磨砂玻璃观感）
 * - 不支持 → 高 alpha 近乎实心底，且不挂 hazeEffect（省掉无效渲染）
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val glassSupported = remember(context) { GlassCompat.isHazeSupported(context) }
    val isDark = isSystemInDarkTheme()
    Surface(
        modifier = modifier,
        shape = shape,
        color = when {
            !glassSupported && isDark -> Color(0xFF1D1B20).copy(alpha = 0.95f)
            !glassSupported -> Color.White.copy(alpha = 0.94f)
            isDark -> Color(0xFF1D1B20).copy(alpha = 0.45f)
            else -> Color.White.copy(alpha = 0.28f)
        },
        contentColor = if (isDark) Color.White else Color(0xFF212121),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .then(
                    if (glassSupported) {
                        Modifier.hazeEffect(state = AppHaze.state, style = HazeMaterials.regular())
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                content = content
            )
        }
    }
}

/**
 * 玻璃对话框基类：无框透明窗口 + ComposeView 承载 [GlassPanel] 内容。
 *
 * 子类只需实现 [Content]；调用方仍按原 DialogFragment 方式 `.show(supportFragmentManager, tag)`。
 *
 * **dim 取值（鸿蒙适配）**：支持模糊时用轻微 dim（0.15）压暗背景、凸显玻璃质感；
 * 不支持模糊（鸿蒙）时改用标准 dim（0.32），配合 [GlassPanel] 的实心底保证可读性。
 */
abstract class GlassDialogFragment : DialogFragment() {

    init {
        setStyle(STYLE_NO_FRAME, 0)
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val cv = ComposeView(requireContext())
        // 注意：不能写成 ComposeView(...).apply { setContent { Content() } }，
        // apply 块内 this 变成 ComposeView，Content() 会歧义解析到 ComposeView.Content()
        // 而非本抽象方法，造成 ComposeView.Content ↔ setContent lambda 无限递归 StackOverflowError。
        cv.setContent { this@GlassDialogFragment.Content() }
        return cv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val glassSupported = GlassCompat.isHazeSupported(requireContext())
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            setDimAmount(if (glassSupported) 0.15f else 0.32f)
        }
    }

    @Composable
    protected abstract fun Content()
}
