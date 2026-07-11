package com.mynote.android.ui.main.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.mynote.android.R

/**
 * 大主题/子主题编辑弹窗（共用）
 */
class CategoryEditDialog(
    private val title: String,
    private val initialName: String,
    private val initialColor: String,
    private val onSave: (name: String, color: String) -> Unit
) : DialogFragment() {

    // 30 色，按彩虹/色相顺序排列
    private val COLORS = arrayOf(
        "#000000", "#FFFFFF", "#9E9E9E", "#607D8B", "#795548",
        "#F44336", "#E91E63", "#FF5252", "#D32F2F", "#B71C1C",
        "#FF5722", "#FF9800", "#FFC107", "#FFEB3B", "#FFD54F",
        "#4CAF50", "#8BC34A", "#00E676", "#1B5E20", "#A5D6A7",
        "#009688", "#00BCD4", "#2196F3", "#03A9F4", "#3F51B5",
        "#9C27B0", "#673AB7", "#E040FB", "#D500F9", "#B39DDB"
    )
    private val dotViews = mutableListOf<View>()
    private var selectedColor = initialColor

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_edit_category, null)
        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_title)
        val etName = view.findViewById<EditText>(R.id.et_name)
        val colorPicker = view.findViewById<GridLayout>(R.id.color_picker)

        tvTitle.text = title
        etName.setText(initialName)
        etName.setSelection(initialName.length)

        // 颜色选择器：30 个颜色圆球，均匀间隔
        val dotSize = 36f.dp
        val marginSize = 5f.dp
        for (color in COLORS) {
            val dot = View(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dotSize
                    height = dotSize
                    setMargins(marginSize, marginSize, marginSize, marginSize)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    setGravity(Gravity.CENTER)
                }
                background = makeDotDrawable(color)
                setOnClickListener {
                    selectedColor = color
                    refreshDots()
                }
            }
            dotViews.add(dot)
            colorPicker.addView(dot)
        }
        refreshDots()

        return AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    onSave(name, selectedColor)
                }
            }
            .setNegativeButton("取消", null)
            .create()
    }

    private fun makeDotDrawable(color: String): GradientDrawable {
        return (ContextCompat.getDrawable(requireContext(), R.drawable.circle_bg)?.mutate() as GradientDrawable).apply {
            setColor(Color.parseColor(color))
            // 白色圆点加浅灰边框，避免在白色背景上看不见
            if (color == "#FFFFFF") {
                setStroke(1f.dp, Color.parseColor("#BDBDBD"))
            } else {
                setStroke(0, Color.TRANSPARENT)
            }
        }
    }

    private fun refreshDots() {
        for (i in COLORS.indices) {
            val dot = dotViews[i]
            val color = COLORS[i]
            val isSelected = selectedColor == color
            dot.scaleX = if (isSelected) 1.25f else 1.0f
            dot.scaleY = if (isSelected) 1.25f else 1.0f
            (dot.background as? GradientDrawable)?.setStroke(
                if (isSelected) 3f.dp else if (color == "#FFFFFF") 1f.dp else 0,
                if (isSelected) Color.WHITE else Color.parseColor("#BDBDBD")
            )
        }
    }

    private val Float.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
