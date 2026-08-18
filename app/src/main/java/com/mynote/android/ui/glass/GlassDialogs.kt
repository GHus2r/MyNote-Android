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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.ComposeView
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
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        modifier = modifier,
        shape = shape,
        color = if (isDark) Color(0xFF1D1B20).copy(alpha = 0.45f)
        else Color.White.copy(alpha = 0.28f),
        contentColor = if (isDark) Color.White else Color(0xFF212121),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .hazeEffect(state = AppHaze.state, style = HazeMaterials.regular())
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
 * 轻微 dim（0.15）压暗背景，让玻璃的模糊/半透明更明显。
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
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            setDimAmount(0.15f)
        }
    }

    @Composable
    protected abstract fun Content()
}
