package com.mynote.android.ui.main.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.mynote.android.R
import com.mynote.android.data.entity.ParentCategory
import com.mynote.android.ui.main.GlassAddCard
import com.mynote.android.ui.main.GlassCategoryCard

class ParentCategoryAdapter(
    private val onItemClick: (ParentCategory) -> Unit,
    private val onEditClick: (ParentCategory) -> Unit,
    private val onDeleteClick: (ParentCategory) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ParentCategory> = emptyList()
    private val noteCounts: MutableMap<String, Int> = mutableMapOf()

    /** 去掉名称中的 emoji 图标，只保留纯文字 */
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

    fun submitList(list: List<ParentCategory>, counts: Map<String, Int>) {
        items = list
        noteCounts.clear()
        noteCounts.putAll(counts)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (position < items.size) TYPE_ITEM else TYPE_ADD

    override fun getItemCount(): Int = items.size + 1 // +1 for add button

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parent_category, parent, false)
        return if (viewType == TYPE_ITEM) ItemVH(view) else AddVH(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemVH) {
            val item = items[position]
            holder.bind(item, noteCounts[item.id] ?: 0)
        }
    }

    inner class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        private val compose = view as ComposeView

        fun bind(item: ParentCategory, count: Int) {
            compose.setContent {
                GlassCategoryCard(
                    name = stripEmoji(item.name),
                    count = count,
                    colorHex = item.color,
                    onClick = { onItemClick(item) },
                    onEdit = { onEditClick(item) },
                    onDelete = { onDeleteClick(item) }
                )
            }
        }
    }

    inner class AddVH(view: View) : RecyclerView.ViewHolder(view) {
        init {
            (view as ComposeView).setContent {
                GlassAddCard(onAdd = { onAddClick() })
            }
        }
    }

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_ADD = 1
    }
}
