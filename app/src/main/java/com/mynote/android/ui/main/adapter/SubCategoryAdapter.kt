package com.mynote.android.ui.main.adapter

import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mynote.android.R
import com.mynote.android.data.entity.SubCategory

class SubCategoryAdapter(
    private val onItemClick: (SubCategory) -> Unit,
    private val onEditClick: (SubCategory) -> Unit,
    private val onDeleteClick: (SubCategory) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<SubCategory> = emptyList()
    private val noteCounts: MutableMap<String, Int> = mutableMapOf()
    private var parentColors: Map<String, String> = emptyMap()

    fun submitList(list: List<SubCategory>, counts: Map<String, Int>, parentColors: Map<String, String> = emptyMap()) {
        items = list
        noteCounts.clear()
        noteCounts.putAll(counts)
        this.parentColors = parentColors
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (position < items.size) TYPE_ITEM else TYPE_ADD

    override fun getItemCount(): Int = items.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sub_category, parent, false)
        return if (viewType == TYPE_ITEM) ItemVH(view) else AddVH(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemVH) {
            holder.bind(items[position], noteCounts[items[position].id] ?: 0)
        }
    }

    inner class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        private val colorDot = view.findViewById<View>(R.id.color_dot)
        private val tvName = view.findViewById<TextView>(R.id.tv_name)
        private val tvCount = view.findViewById<TextView>(R.id.tv_count)
        private val btnEdit = view.findViewById<View>(R.id.btn_edit)
        private val btnDelete = view.findViewById<View>(R.id.btn_delete)

        fun bind(item: SubCategory, count: Int) {
            tvName.text = item.name
            tvCount.text = "${count} 篇"
            val colorStr = try {
                item.color.takeIf { it.isNotBlank() }
                    ?: parentColors[item.parentId]
                    ?: "#2196F3"
            } catch (_: Exception) { "#2196F3" }
            val baseColor = try { Color.parseColor(colorStr) } catch (_: Exception) { Color.parseColor("#2196F3") }
            (colorDot.background as? GradientDrawable)?.setColor(baseColor)

            // 子主题卡片真玻璃背景（透主题色淡光斑）
            itemView.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.sub_card_glass)
                ?.setContent {
                    com.mynote.android.ui.glass.GlassCardBackground(tintHex = colorStr)
                }

            itemView.setOnClickListener { onItemClick(item) }
            btnEdit.setOnClickListener { onEditClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    inner class AddVH(view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.findViewById<View>(R.id.btn_edit).visibility = View.GONE
            view.findViewById<View>(R.id.btn_delete).visibility = View.GONE
            view.findViewById<TextView>(R.id.tv_name).text = "+ 添加子主题"
            view.findViewById<TextView>(R.id.tv_name).setTextColor(Color.GRAY)
            view.findViewById<TextView>(R.id.tv_count).visibility = View.GONE
            itemView.setOnClickListener { onAddClick() }
        }
    }

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_ADD = 1
    }
}
