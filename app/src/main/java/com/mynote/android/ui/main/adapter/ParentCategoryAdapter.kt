package com.mynote.android.ui.main.adapter

import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mynote.android.R
import com.mynote.android.data.entity.ParentCategory

class ParentCategoryAdapter(
    private val onItemClick: (ParentCategory) -> Unit,
    private val onEditClick: (ParentCategory) -> Unit,
    private val onDeleteClick: (ParentCategory) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ParentCategory> = emptyList()
    private val noteCounts: MutableMap<String, Int> = mutableMapOf()
    private val COLORS = arrayOf("#2196F3", "#4CAF50", "#FF9800", "#F44336", "#9C27B0", "#00BCD4", "#FF5722", "#607D8B")

    /** 去掉名称中的 emoji 图标，只保留纯文字 */
    private fun stripEmoji(name: String): String {
        val sb = StringBuilder()
        name.codePoints().forEach { cp ->
            // 保留：CJK、ASCII字母数字、基本标点、空格；过滤 emoji 和特殊符号
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
        // AddVH: click is handled in init
    }

    inner class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        private val card = view.findViewById<View>(R.id.card_content)
        private val colorBlock = view.findViewById<View>(R.id.color_block)
        private val tvName = view.findViewById<TextView>(R.id.tv_name)
        private val tvCount = view.findViewById<TextView>(R.id.tv_count)
        private val btnEdit = view.findViewById<View>(R.id.btn_edit)
        private val btnDelete = view.findViewById<View>(R.id.btn_delete)

        fun bind(item: ParentCategory, count: Int) {
            tvName.text = stripEmoji(item.name)
            tvCount.text = "${count} 篇"
            itemView.findViewById<View>(R.id.card_add).visibility = View.GONE
            itemView.findViewById<View>(R.id.card_content).visibility = View.VISIBLE
            val baseColor = try { Color.parseColor(item.color) } catch (_: Exception) { Color.parseColor("#2196F3") }
            (colorBlock.background.mutate() as? android.graphics.drawable.GradientDrawable)?.setColor(baseColor)
            card.setOnClickListener { onItemClick(item) }
            btnEdit.setOnClickListener { onEditClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    inner class AddVH(view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.findViewById<View>(R.id.card_content).visibility = View.GONE
            view.findViewById<View>(R.id.card_add).visibility = View.VISIBLE
            view.findViewById<View>(R.id.card_add).setOnClickListener { onAddClick() }
        }
    }

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_ADD = 1
    }
}
