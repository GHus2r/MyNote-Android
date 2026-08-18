package com.mynote.android.ui.main.dialog

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynote.android.data.entity.ParentCategory
import com.mynote.android.ui.glass.GlassDialogFragment
import com.mynote.android.ui.glass.GlassPanel

/**
 * 移动子主题弹窗 — Haze 玻璃材质版
 */
class MoveSubDialog(
    categories: List<ParentCategory>,
    excludeParentId: String,
    private val onSelect: (ParentCategory) -> Unit
) : GlassDialogFragment() {

    private val targets = categories.filter { it.id != excludeParentId }

    @Composable
    override fun Content() {
        GlassPanel {
            Text(
                text = "选择目标大主题",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF212121)
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(targets, key = { it.id }) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .clickable {
                                onSelect(category)
                                dismiss()
                            }
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    runCatching { Color(android.graphics.Color.parseColor(category.color)) }
                                        .getOrDefault(Color(0xFF2196F3))
                                )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = category.name,
                            fontSize = 15.sp,
                            color = if (isSystemInDarkTheme()) Color(0xFFE0E0E0) else Color(0xFF424242)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { dismiss() }) {
                    Text("取消", color = Color(0xFF757575))
                }
            }
        }
    }
}
