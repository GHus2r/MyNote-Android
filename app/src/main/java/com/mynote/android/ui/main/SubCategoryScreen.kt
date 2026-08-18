package com.mynote.android.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mynote.android.data.entity.SubCategory
import com.mynote.android.ui.glass.AppHaze
import dev.chrisbanes.haze.hazeSource

data class SubCategoryUiState(
    val items: List<SubCategory> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val parentColors: Map<String, String> = emptyMap()
)

@Composable
fun SubCategoryScreen(
    state: SubCategoryUiState,
    onItemClick: (SubCategory) -> Unit,
    onEditClick: (SubCategory) -> Unit,
    onDeleteClick: (SubCategory) -> Unit,
    onAddClick: () -> Unit
) {
    val backdrop = rememberLayerBackdrop { }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .hazeSource(AppHaze.state),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 64.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.items, key = { it.id }) { item ->
                SubCategoryCard(
                    item = item,
                    count = state.counts[item.id] ?: 0,
                    colorHex = item.color.takeIf { it.isNotBlank() }
                        ?: state.parentColors[item.parentId] ?: "#2196F3",
                    backdrop = backdrop,
                    onClick = { onItemClick(item) },
                    onEdit = { onEditClick(item) },
                    onDelete = { onDeleteClick(item) }
                )
            }
            item(key = "add") {
                AddSubCategoryCard(
                    backdrop = backdrop,
                    onClick = onAddClick
                )
            }
        }

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

@Composable
private fun SubCategoryCard(
    item: SubCategory,
    count: Int,
    colorHex: String,
    backdrop: Backdrop,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val color = parseSubColor(colorHex)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(6f.dp.toPx(), 14f.dp.toPx())
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = item.name,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )
            Text(
                text = "${count} 篇",
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
            Text(
                text = "E",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(onClick = onEdit),
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
            Text(
                text = "D",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(onClick = onDelete),
                fontSize = 12.sp,
                color = Color(0xFFE53935)
            )
        }
    }
}

@Composable
private fun AddSubCategoryCard(
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(6f.dp.toPx(), 14f.dp.toPx())
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "+ 添加子主题",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757575)
        )
    }
}

private fun parseSubColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF2196F3)
}
