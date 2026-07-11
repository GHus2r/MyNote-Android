package com.mynote.android.ui.main.dialog

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.mynote.android.util.BackgroundHelper

class BackgroundPickerDialog(
    private val activity: AppCompatActivity,
    private val pickImage: (() -> Unit)? = null   // 由外部传入图片选择触发
) : BottomSheetDialog(activity) {

    private var currentMode = BackgroundHelper.getMode(activity)
    private lateinit var tabLayout: TabLayout
    private lateinit var contentContainer: LinearLayout
    private val density by lazy { context.resources.displayMetrics.density }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(createContent())
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setDimAmount(0.4f)

        setOnShowListener {
            val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED }
        }
    }

    private fun createContent(): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setPadding(0, dp(12), 0, dp(16))
        }

        val title = TextView(context).apply {
            text = "主页背景"
            textSize = 16f
            setTextColor(Color.parseColor("#212121"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(title)

        tabLayout = TabLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), 0, dp(16), 0)
            }
            tabMode = TabLayout.MODE_FIXED
            tabGravity = TabLayout.GRAVITY_FILL
            addTab(newTab().setText("渐变"))
            addTab(newTab().setText("自定义"))

            val idx = when (currentMode) {
                "image" -> 1; else -> 0
            }
            getTabAt(idx)?.select()

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    contentContainer.post { showTabContent(tab?.position ?: 0) }
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
        root.addView(tabLayout)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0).apply { weight = 1f }
        }
        contentContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        scrollView.addView(contentContainer)
        root.addView(scrollView)

        contentContainer.post { showTabContent(tabLayout.selectedTabPosition) }
        return root
    }

    private fun showTabContent(position: Int) {
        if (!::contentContainer.isInitialized) return
        contentContainer.removeAllViews()
        when (position) {
            0 -> showGradientTab()
            1 -> showImageTab()
        }
    }

    private fun showGradientTab() {
        for (grad in BackgroundHelper.presetGradients) {
            val sel = currentMode == "gradient" && BackgroundHelper.getValue(activity) == "${grad.startHex}_${grad.endHex}"
            val thumb = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor(grad.startHex), Color.parseColor(grad.endHex))
            ).apply { cornerRadius = dp(4).toFloat() }
            contentContainer.addView(buildItem(grad.name, "${grad.startHex} → ${grad.endHex}", thumb, sel) {
                BackgroundHelper.setGradient(activity, grad.startHex, grad.endHex)
                currentMode = "gradient"; dismiss()
            })
        }
    }

    private fun showImageTab() {
        val hint = TextView(context).apply {
            text = "从相册选择一张图片作为主页背景\n图片将被拉伸填充整个屏幕"
            textSize = 13f; setTextColor(Color.parseColor("#757575"))
            gravity = Gravity.CENTER; setPadding(0, dp(24), 0, dp(24))
        }
        contentContainer.addView(hint)

        val btn = TextView(context).apply {
            text = "从相册选择"; textSize = 15f
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(dp(24), dp(12), dp(24), dp(12))
            background = GradientDrawable().apply { setColor(Color.parseColor("#4CAF50")); cornerRadius = dp(8).toFloat() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setOnClickListener { pickImage?.invoke(); dismiss() }
        }
        contentContainer.addView(btn)

        val path = BackgroundHelper.getImagePath(activity)
        if (path != null) {
            val clear = TextView(context).apply {
                text = "清除背景图片"; textSize = 14f
                setTextColor(Color.parseColor("#F44336")); gravity = Gravity.CENTER
                setPadding(0, dp(20), 0, 0)
                setOnClickListener { BackgroundHelper.setDefault(activity); currentMode = "default"; dismiss() }
            }
            contentContainer.addView(clear)
        }
    }

    private fun buildItem(title: String, subtitle: String?, thumb: Drawable?, selected: Boolean, onClick: () -> Unit): View {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE; cornerRadius = dp(8).toFloat()
            setColor(Color.WHITE)
            if (selected) setStroke(dp(2), Color.parseColor("#4CAF50"))
            else setStroke(dp(1), Color.parseColor("#E0E0E0"))
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12)); background = bg
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
            setOnClickListener { onClick() }
        }
        if (thumb != null) {
            val v = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(12) }
                background = thumb
            }
            container.addView(v)
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tv = TextView(context).apply { text = title; textSize = 14f; setTextColor(Color.parseColor("#212121")) }
        col.addView(tv)
        if (subtitle != null) {
            val ts = TextView(context).apply { text = subtitle; textSize = 12f; setTextColor(Color.parseColor("#9E9E9E")) }
            col.addView(ts)
        }
        container.addView(col)
        return container
    }

    private fun dp(v: Int): Int = (v * density + 0.5f).toInt()
}
