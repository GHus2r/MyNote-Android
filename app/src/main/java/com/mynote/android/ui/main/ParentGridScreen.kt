package com.mynote.android.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.mynote.android.data.entity.ParentCategory
import com.mynote.android.ui.glass.AppHaze
import dev.chrisbanes.haze.hazeSource

/**
 * 主页「大主题网格 + 玻璃顶栏」（backdrop 真玻璃，透出滚动内容）。
 *
 * 结构：Box { LazyVerticalGrid(layerBackdrop) + 顶部玻璃条(drawBackdrop) }
 * 网格向上滚动经过玻璃条时，玻璃条实时透出模糊的卡片内容。
 *
 * 顶栏按钮（返回/标题/搜索/病历/工具/设置）是 View 层，透明叠在本组件上方（见 activity_main.xml）。
 */
@Composable
fun ParentGridScreen(
    parents: List<ParentCategory>,
    counts: Map<String, Int>,
    onItemClick: (ParentCategory) -> Unit,
    onEditClick: (ParentCategory) -> Unit,
    onDeleteClick: (ParentCategory) -> Unit,
    onAddClick: () -> Unit
) {
    val backdrop = rememberLayerBackdrop { }

    Box(Modifier.fillMaxSize()) {
        // 1) 网格内容（被玻璃透出的内容；hazeSource 注册为对话框玻璃的模糊源）
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .hazeSource(AppHaze.state),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 64.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(parents, key = { it.id }) { p ->
                GlassCategoryCard(
                    name = stripEmoji(p.name),
                    count = counts[p.id] ?: 0,
                    colorHex = p.color,
                    onClick = { onItemClick(p) },
                    onEdit = { onEditClick(p) },
                    onDelete = { onDeleteClick(p) }
                )
            }
            item(key = "add") {
                GlassAddCard(onAdd = onAddClick)
            }
        }

        // 2) 玻璃顶栏（透出滚动的网格内容）
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(48.dp)
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

/** 去掉名称中的 emoji 图标，只保留纯文字（与原 ParentCategoryAdapter 一致） */
private fun stripEmoji(name: String): String {
    val sb = StringBuilder()
    name.codePoints().forEach { cp ->
        if (cp !in 0x1F000..0x1FFFF && cp !in 0x2600..0x27BF &&
            cp !in 0x2300..0x23FF && cp !in 0x2B50..0x2B55 &&
            cp !in 0x2702..0x27B0 && cp != 0xFE0F && cp != 0x200D &&
            cp != 0x20E3 && cp != 0xFE0E &&
            !(cp in 0xD800..0xDFFF) &&
            cp != 0x270F && cp != 0x2712 && cp != 0x2714
        ) {
            sb.append(Character.toChars(cp))
        }
    }
    return sb.toString().trim()
}
