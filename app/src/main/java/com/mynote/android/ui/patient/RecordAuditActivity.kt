package com.mynote.android.ui.patient

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mynote.android.R
import com.mynote.android.util.BackgroundHelper
import com.mynote.android.util.MedicalRecordAuditor
import com.mynote.android.util.Prefs
import com.mynote.android.util.RecordScorer
import kotlinx.coroutines.launch

/**
 * 病历照片质控分析页
 * 输入：病历照片（相册） + 科室 + 文书类型
 * 流程：OCR 识图 → 规范性/准确性/漏写项分析 → 可操作改进方案
 * 纯代码式 UI，独立 Activity，不改动任何现有页面。
 */
class RecordAuditActivity : AppCompatActivity() {

    private companion object {
        val TEXT_PRIMARY = 0xFF212121.toInt()
        val TEXT_SECONDARY = 0xFF757575.toInt()
        val CARD_BG = 0xFFF5F5F5.toInt()
        val ACCENT = 0xFF1B873F.toInt()
        val RED = 0xFFE53935.toInt()
        val AMBER = 0xFFF9A825.toInt()
        val BLUE = 0xFF1565C0.toInt()
        val LINE = 0xFFE0E0E0.toInt()
    }

    private var patientId: Long = 0
    private val selectedBitmaps: MutableList<Bitmap> = mutableListOf()

    private lateinit var etDept: EditText
    private lateinit var etRecordType: EditText
    private lateinit var etPaste: EditText
    private lateinit var tvImageStatus: TextView
    private lateinit var previewRow: LinearLayout
    private lateinit var inputCard: LinearLayout
    private lateinit var btnAnalyze: Button
    private lateinit var progress: ProgressBar
    private lateinit var resultContainer: LinearLayout

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) onImagesPicked(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        patientId = intent.getLongExtra("patient_id", 0)
        setContentView(buildContentView())
    }

    private fun buildContentView(): View {
        val scroll = ScrollView(this).apply {
            background = resolveRootBg()
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 顶部栏
        val topbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 16, 16, 12)
        }
        val btnBack = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { finish() }
        }
        val tvTitle = TextView(this).apply {
            text = "病历照片质控"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT_PRIMARY)
            setPadding(8, 0, 0, 0)
        }
        topbar.addView(btnBack, LinearLayout.LayoutParams(dp(48), dp(48)))
        topbar.addView(tvTitle, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(topbar)

        // 免责声明
        root.addView(TextView(this).apply {
            text = "本结果由 AI 生成，仅供临床质控参考，不构成诊疗决策依据，最终以执业医师判断为准。"
            textSize = 12f
            setTextColor(AMBER)
            setPadding(16, 6, 16, 6)
            setBackgroundColor(0xFFFFF8E1.toInt())
        })

        // 输入区
        inputCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
        }
        inputCard.addView(label("科室（用于匹配专科判定依据，可留空）"))
        etDept = EditText(this).apply {
            hint = "如：消化科"
            textSize = 15f
            setSingleLine(true)
        }
        inputCard.addView(etDept)

        inputCard.addView(label("文书类型"))
        etRecordType = EditText(this).apply {
            setText("入院记录")
            textSize = 15f
            setSingleLine(true)
        }
        inputCard.addView(etRecordType)

        inputCard.addView(label("病历文字（可选：粘贴后优先按文字分析，留空则用照片识别）"))
        etPaste = EditText(this).apply {
            hint = "可在此粘贴病历全文（如从电子病历复制）"
            textSize = 14f
            gravity = Gravity.TOP
            minLines = 4
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = glassCardBg()
        }
        inputCard.addView(etPaste, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(140)))

        inputCard.addView(label("病历照片（可多选，多页病历逐页拍）"))
        val pickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tvImageStatus = TextView(this).apply {
            text = "尚未选择照片"
            textSize = 13f
            setTextColor(TEXT_SECONDARY)
            setPadding(dp(12), 0, 0, 0)
        }
        val btnPick = Button(this).apply {
            text = "从相册选择（可多选）"
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            background = roundedBtn(BLUE, 24)
            setOnClickListener { pickImages.launch("image/*") }
        }
        val btnClear = Button(this).apply {
            text = "清空"
            textSize = 13f
            setTextColor(TEXT_SECONDARY)
            background = glassCardBg()
            setOnClickListener {
                selectedBitmaps.clear()
                refreshPreview()
            }
        }
        pickRow.addView(btnPick)
        pickRow.addView(btnClear)
        pickRow.addView(tvImageStatus, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        inputCard.addView(pickRow)
        previewRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val previewScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        previewScroll.addView(previewRow)
        inputCard.addView(previewScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        root.addView(inputCard)

        // 分析按钮
        btnAnalyze = Button(this).apply {
            text = "开始质控分析"
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            background = roundedBtn(ACCENT, 28)
            setOnClickListener { startAnalyze() }
        }
        root.addView(btnAnalyze, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
            setMargins(16, 8, 16, 8)
        })

        // 进度
        progress = ProgressBar(this).apply {
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)))

        // 结果区
        resultContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 24)
            visibility = View.GONE
        }
        root.addView(resultContainer)

        scroll.addView(root)
        return scroll
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(TEXT_SECONDARY)
        setPadding(0, dp(10), 0, dp(4))
    }

    private fun onImagesPicked(uris: List<Uri>) {
        var loaded = 0
        uris.forEach { uri ->
            val bmp = loadBitmap(uri)
            if (bmp != null) {
                selectedBitmaps.add(bmp)
                loaded++
            }
        }
        if (loaded == 0) {
            Toast.makeText(this, "图片读取失败", Toast.LENGTH_SHORT).show()
            return
        }
        refreshPreview()
    }

    private fun refreshPreview() {
        previewRow.removeAllViews()
        tvImageStatus.text = if (selectedBitmaps.isEmpty()) "尚未选择照片" else "已选择 ${selectedBitmaps.size} 张照片"
        selectedBitmaps.forEach { bmp ->
            previewRow.addView(ImageView(this).apply {
                setImageBitmap(bmp)
                scaleType = ImageView.ScaleType.FIT_CENTER
                background = glassCardBg()
                setPadding(dp(2), 0, dp(2), 0)
            }, LinearLayout.LayoutParams(dp(96), dp(128)).apply {
                setMargins(0, 0, dp(6), 0)
            })
        }
    }

    private fun startAnalyze() {
        val pasted = etPaste.text.toString().trim()
        val dept = etDept.text.toString().trim()
        val type = etRecordType.text.toString().trim().ifEmpty { "入院记录" }

        if (pasted.isEmpty() && selectedBitmaps.isEmpty()) {
            Toast.makeText(this, "请上传照片或粘贴病历文字", Toast.LENGTH_SHORT).show()
            return
        }

        if (Prefs(this).qwenApiKey.isBlank()) {
            AlertDialog.Builder(this, R.style.GlassDialog)
                .setTitle("未配置 AI Key")
                .setMessage("请先在「设置」中配置 Qwen（百炼）API Key，再使用病历质控功能。")
                .setPositiveButton("知道了", null)
                .show()
            return
        }

        btnAnalyze.isEnabled = false
        btnAnalyze.text = "分析中…"
        progress.visibility = View.VISIBLE
        resultContainer.visibility = View.GONE

        lifecycleScope.launch {
            val res = if (pasted.isNotEmpty()) {
                MedicalRecordAuditor.auditText(this@RecordAuditActivity, pasted, dept, type, patientId)
            } else {
                MedicalRecordAuditor.audit(this@RecordAuditActivity, selectedBitmaps.toList(), dept, type, patientId)
            }
            btnAnalyze.isEnabled = true
            btnAnalyze.text = "开始质控分析"
            progress.visibility = View.GONE
            res.onSuccess { r ->
                inputCard.visibility = View.GONE
                btnAnalyze.visibility = View.GONE
                resultContainer.visibility = View.VISIBLE
                renderResult(r)
            }
            res.onFailure { e ->
                AlertDialog.Builder(this@RecordAuditActivity, R.style.GlassDialog)
                    .setTitle("分析失败")
                    .setMessage(e.message ?: "未知错误，请检查网络后重试")
                    .setPositiveButton("知道了", null)
                    .show()
            }
        }
    }

    // ---------- 结果渲染 ----------

    private fun renderResult(r: MedicalRecordAuditor.AuditResult) {
        resultContainer.removeAllViews()

        // 重新分析
        val btnReset = Button(this).apply {
            text = "← 重新分析"
            textSize = 13f
            setTextColor(BLUE)
            background = glassCardBg()
            setOnClickListener { resetToInput() }
        }
        resultContainer.addView(btnReset)

        // 总分卡片
        val scoreCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardBg()
            setPadding(16, 16, 16, 16)
        }
        val color = RecordScorer.colorHex(r.scoreTotal).let { parseColor(it) }
        scoreCard.addView(TextView(this).apply {
            text = "${r.scoreTotal}"
            textSize = 40f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color)
            gravity = Gravity.CENTER
        })
        scoreCard.addView(TextView(this).apply {
            text = RecordScorer.starLabel(r.scoreTotal)
            textSize = 16f
            setTextColor(color)
            gravity = Gravity.CENTER
        })
        scoreCard.addView(TextView(this).apply {
            text = "完整性(漏写) ${r.scoreCompleteness}/40  规范性 ${r.scoreNorms}/30  准确性 ${r.scoreAccuracy}/30"
            textSize = 13f
            setTextColor(TEXT_SECONDARY)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
        if (r.summary.isNotEmpty()) {
            scoreCard.addView(TextView(this).apply {
                text = r.summary
                textSize = 14f
                setTextColor(TEXT_PRIMARY)
                setPadding(0, dp(8), 0, 0)
            })
        }
        resultContainer.addView(scoreCard)

        // 查看识别原文
        val btnRaw = Button(this).apply {
            text = "查看 OCR 识别原文"
            textSize = 13f
            setTextColor(BLUE)
            background = glassCardBg()
            setOnClickListener {
                AlertDialog.Builder(this@RecordAuditActivity, R.style.GlassDialog)
                    .setTitle("识别原文（可溯源）")
                    .setMessage(r.ocrRawText.ifEmpty { "(空)" })
                    .setPositiveButton("关闭", null)
                    .show()
            }
        }
        resultContainer.addView(btnRaw)

        // 漏写项
        addSection("漏写项（${r.missingItems.size}）", RED)
        if (r.missingItems.isEmpty()) {
            addEmpty("未发现漏写项")
        } else {
            r.missingItems.forEach { item ->
                val sevColor = if (item.severity == "必填") RED else AMBER
                addIssueCard(
                    "【${item.severity}】${item.title}",
                    listOfNotNull(
                        item.basis.takeIf { it.isNotEmpty() }?.let { "依据：$it" },
                        item.suggestion.takeIf { it.isNotEmpty() }?.let { "建议：$it" }
                    ).joinToString("\n"),
                    sevColor
                )
            }
        }

        // 规范性问题
        addSection("规范性问题（${r.normIssues.size}）", BLUE)
        if (r.normIssues.isEmpty()) {
            addEmpty("未发现规范性问题")
        } else {
            r.normIssues.forEach { item ->
                addIssueCard(
                    item.title,
                    listOfNotNull(
                        item.detail.takeIf { it.isNotEmpty() },
                        item.basis.takeIf { it.isNotEmpty() }?.let { "依据：$it" },
                        item.suggestion.takeIf { it.isNotEmpty() }?.let { "修正：$it" }
                    ).joinToString("\n"),
                    BLUE
                )
            }
        }

        // 准确性问题
        addSection("准确性问题（${r.accuracyIssues.size}）", AMBER)
        if (r.accuracyIssues.isEmpty()) {
            addEmpty("未发现准确性问题")
        } else {
            r.accuracyIssues.forEach { item ->
                addIssueCard(
                    item.title,
                    listOfNotNull(
                        item.detail.takeIf { it.isNotEmpty() },
                        item.evidence.takeIf { it.isNotEmpty() }?.let { "原文：$it" },
                        item.suggestion.takeIf { it.isNotEmpty() }?.let { "建议：$it" },
                        item.refGuideline.takeIf { it.isNotEmpty() }?.let { "指南：$it" }
                    ).joinToString("\n"),
                    AMBER
                )
            }
        }

        // 改进方案
        addSection("改进方案（${r.improvementPlan.size}）", ACCENT)
        if (r.improvementPlan.isEmpty()) {
            addEmpty("暂无改进方案")
        } else {
            r.improvementPlan.forEachIndexed { i, p ->
                addIssueCard(
                    "${i + 1}. ${p.step}",
                    listOfNotNull(
                        p.action.takeIf { it.isNotEmpty() }?.let { "动作：$it" },
                        p.target.takeIf { it.isNotEmpty() }?.let { "目标：$it" },
                        p.rationale.takeIf { it.isNotEmpty() }?.let { "理由：$it" }
                    ).joinToString("\n"),
                    ACCENT
                )
            }
        }
    }

    private fun resetToInput() {
        resultContainer.visibility = View.GONE
        inputCard.visibility = View.VISIBLE
        btnAnalyze.visibility = View.VISIBLE
        resultContainer.removeAllViews()
    }

    private fun addSection(title: String, color: Int) {
        resultContainer.addView(TextView(this).apply {
            text = title
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color)
            setPadding(0, dp(16), 0, dp(6))
        })
    }

    private fun addEmpty(text: String) {
        resultContainer.addView(TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(TEXT_SECONDARY)
            setPadding(0, 0, 0, dp(8))
        })
    }

    private fun addIssueCard(title: String, body: String, color: Int) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardBg()
            setPadding(14, 10, 14, 10)
        }
        card.addView(TextView(this).apply {
            text = title
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color)
        })
        if (body.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = body
                textSize = 13f
                setTextColor(TEXT_PRIMARY)
                setPadding(0, dp(4), 0, 0)
            })
        }
        resultContainer.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dp(8))
        })
    }

    // ---------- 工具 ----------

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxDim / sample > 2000) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseColor(hex: String): Int = try { Color.parseColor(hex) } catch (e: Exception) { ACCENT }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ---------- 视觉辅助（与项目其他页面统一：玻璃/圆角） ----------

    /** 根背景：跟随用户在「设置-背景」里的选择（颜色/渐变/图片），默认浅灰 */
    private fun resolveRootBg(): Drawable {
        return BackgroundHelper.getBackground(this) ?: ColorDrawable(Color.parseColor("#F8F8F8"))
    }

    /** 玻璃卡片背景：半透明白 + 14dp 圆角 + 1dp 描边（与项目 dialog_glass_bg 同语系） */
    private fun glassCardBg(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(0xF0FFFFFF.toInt())
            setCornerRadius(dp(14).toFloat())
            setStroke(dp(1), 0xFFE0E0E0.toInt())
        }
    }

    /** 实心圆角按钮 */
    private fun roundedBtn(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            setCornerRadius(dp(radiusDp).toFloat())
        }
    }
}
