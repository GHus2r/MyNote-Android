package com.mynote.android.ui.main.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mynote.android.R
import com.mynote.android.data.entity.ParentCategory

/**
 * 移动子主题弹窗
 */
class MoveSubDialog(
    private val categories: List<ParentCategory>,
    private val excludeParentId: String,
    private val onSelect: (ParentCategory) -> Unit
) : DialogFragment() {

    private val targets = categories.filter { it.id != excludeParentId }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_move_sub, null)
        val recycler = view.findViewById<RecyclerView>(R.id.rv_targets)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = object : RecyclerView.Adapter<VH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                return VH(itemView)
            }
            override fun onBindViewHolder(holder: VH, position: Int) {
                holder.bind(targets[position])
            }
            override fun getItemCount(): Int = targets.size
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("选择目标大主题")
            .setView(view)
            .setNegativeButton("取消", null)
            .create()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(category: ParentCategory) {
            (itemView as TextView).text = category.name
            itemView.setOnClickListener { onSelect(category); dismiss() }
        }
    }
}
