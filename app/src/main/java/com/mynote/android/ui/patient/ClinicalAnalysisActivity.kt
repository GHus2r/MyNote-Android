package com.mynote.android.ui.patient

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.mynote.android.R
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.MedicalRecord
import com.mynote.android.data.entity.Patient
import com.mynote.android.data.entity.VitalSigns
import com.mynote.android.ui.image.ImageCropActivity
import com.mynote.android.util.ClinicalAnalysisResult
import com.mynote.android.util.ClinicalCaseBuilder
import com.mynote.android.util.ClinicalReasoningClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.io.File

/**
 * AI 临床辅助分析 — 多源融合推理结果展示页
 * 输入：病史(病历) + 化验(VitalSigns) + 影像报告文本 → 融合推理 → 鉴别诊断 + 诊疗建议（证据锚定）
 */
class ClinicalAnalysisActivity : AppCompatActivity() {

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

    private val db by lazy { AppDatabase.get(this) }
    private var patientId: Long = 0
    private var patient: Patient? = null
    private lateinit var etImaging: EditText
    private lateinit var resultContainer: LinearLayout
    private lateinit var btnSave: Button
    private lateinit var tvPatient: TextView
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var cropLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var photoUri: Uri? = null
    private var lastResult: ClinicalAnalysisResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        patientId = intent.getLongExtra("patient_id", 0)
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) startCrop(uri)
        }
        cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.getStringExtra("cropped_path")?.let { ocrCropped(it) }
            }
        }
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = photoUri
            if (success && uri != null) startCrop(uri)
        }
        setContentView(buildContentView())
        loadPatient()
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
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
            text = "AI 临床辅助分析"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT_PRIMARY)
            setPadding(8, 0, 0, 0)
        }
        topbar.addView(btnBack, LinearLayout.LayoutParams(dp(48), dp(48)))
        topbar.addView(tvTitle, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        // 免责声明
        val tvDisclaimer = TextView(this).apply {
            text = "本结果由 AI 生成，仅供临床参考，不构成诊疗决策依据，最终以执业医师判断为准。"
            textSize = 12f
            setTextColor(AMBER)
            setPadding(16, 6, 16, 6)
            setBackgroundColor(0xFFFFF8E1.toInt())
        }

        // 内容
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 24)
        }

        // 患者信息卡
        val patientCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(CARD_BG)
        }
        tvPatient = TextView(this).apply {
            text = "患者信息加载中…"
            textSize = 14f
            setTextColor(TEXT_PRIMARY)
        }
        patientCard.addView(tvPatient)

        // 影像报告输入
        val tvLabelRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 6)
        }
        val tvLabel = TextView(this).apply {
            text = "影像报告（可选，粘贴/输入文字或导入照片）"
            textSize = 13f
            setTextColor(TEXT_SECONDARY)
        }
        val btnImportPhoto = Button(this).apply {
            text = "📷 导入照片"
            textSize = 12f
            setTextColor(ACCENT)
            setBackgroundColor(0xFFE8F5E9.toInt())
            setPadding(dp(16), dp(4), dp(16), dp(4))
            setOnClickListener { showImageSourceDialog() }
        }
        tvLabelRow.addView(tvLabel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        tvLabelRow.addView(btnImportPhoto)

        etImaging = EditText(this).apply {
            hint = "例：上腹部 CT 提示胰腺肿胀、胰周渗出，符合急性胰腺炎表现…"
            minLines = 3
            maxLines = 10
            setHorizontallyScrolling(false)
            setPadding(16, 12, 16, 12)
            setBackgroundColor(CARD_BG)
            setTextColor(TEXT_PRIMARY)
            textSize = 14f
            gravity = Gravity.TOP
        }

        val btnAnalyze = Button(this).apply {
            text = "开始分析"
            setTextColor(Color.WHITE)
            setBackgroundColor(ACCENT)
            setOnClickListener { doAnalyze() }
        }

        btnSave = Button(this).apply {
            text = "保存为病历"
            setTextColor(ACCENT)
            setBackgroundColor(0xFFE8F5E9.toInt())
            visibility = View.GONE
            setOnClickListener { saveToRecord() }
        }

        resultContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(patientCard)
        content.addView(tvLabelRow)
        content.addView(etImaging)
        content.addView(btnAnalyze, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        content.addView(btnSave, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        content.addView(resultContainer)

        scroll.addView(content)

        root.addView(topbar)
        root.addView(tvDisclaimer)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun loadPatient() {
        lifecycleScope.launch(Dispatchers.IO) {
            val p = db.patientDao().getById(patientId)
            withContext(Dispatchers.Main) {
                patient = p
                if (p == null) {
                    tvPatient.text = "未找到患者"
                } else {
                    tvPatient.text = buildString {
                        append("${p.age}岁 · ${p.gender}")
                        if (p.department.isNotEmpty()) append(" · ${p.department}")
                        if (p.diagnosis.isNotEmpty()) append("\n诊断：${p.diagnosis}")
                        if (p.chiefComplaint.isNotEmpty()) append("\n主诉：${p.chiefComplaint}")
                    }
                }
            }
        }
    }

    /** 选择图片来源：拍照 / 相册 */
    private fun showImageSourceDialog() {
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("导入影像报告")
            .setItems(arrayOf("拍照", "从相册选择")) { _, which ->
                if (which == 0) takePhoto() else pickImageLauncher.launch("image/*")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 拍照（走系统相机，FileProvider 提供 URI），拍完自动跳裁剪 */
    private fun takePhoto() {
        // 检查是否有相机应用（模拟器/部分设备可能没有，直接 launch 会抛 ActivityNotFoundException）
        val captureIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        if (captureIntent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "未检测到相机应用，请从相册选择", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val file = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            photoUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "无法启动相机：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** 选图后跳转裁剪 */
    private fun startCrop(uri: Uri) {
        val intent = Intent(this, ImageCropActivity::class.java).putExtra("image_uri", uri.toString())
        cropLauncher.launch(intent)
    }

    /** 裁剪返回后 OCR 裁剪图并填入输入框 */
    private fun ocrCropped(path: String) {
        Toast.makeText(this, "识别中…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val text = try { ocrBitmapFromFile(path) } catch (e: Exception) { null }
            if (text.isNullOrBlank()) {
                Toast.makeText(this@ClinicalAnalysisActivity, "未识别到文字，请手动输入", Toast.LENGTH_LONG).show()
            } else {
                // 若已有内容则追加（用换行分隔），否则覆盖
                val cur = etImaging.text.toString().trim()
                etImaging.setText(if (cur.isEmpty()) text else "$cur\n$text")
                etImaging.setSelection(etImaging.text.length)
                Toast.makeText(this@ClinicalAnalysisActivity, "已导入（可手动修改）", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** ML Kit 中文 OCR 识别裁剪后的图片文件 */
    private suspend fun ocrBitmapFromFile(path: String): String? = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(path) ?: return@withContext null
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            val text = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text?> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
            text?.text
        } finally {
            bitmap.recycle()
        }
    }

    private fun doAnalyze() {
        val p = patient ?: run { Toast.makeText(this, "患者信息未加载", Toast.LENGTH_SHORT).show(); return }
        val imaging = etImaging.text.toString().trim()

        // 清空旧结果
        resultContainer.removeAllViews()
        btnSave.visibility = View.GONE
        lastResult = null

        val loading = TextView(this).apply {
            text = "🧠 正在融合分析…"
            textSize = 14f
            setTextColor(TEXT_SECONDARY)
            setPadding(0, 16, 0, 0)
        }
        resultContainer.addView(loading)

        lifecycleScope.launch {
            var localCritical: List<ClinicalAnalysisResult.CriticalAlert> = emptyList()
            var records: List<MedicalRecord> = emptyList()
            var vital: VitalSigns? = null
            try {
                records = db.medicalRecordDao().getByPatientSync(patientId)
                vital = db.vitalSignsDao()
                    .getSince(patientId, System.currentTimeMillis() - 90L * 24 * 3600 * 1000)
                    .lastOrNull()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultContainer.removeAllViews()
                    addError("数据加载失败：${e.message}")
                }
                return@launch
            }

            // 数据空守卫：病历/化验/影像三者全空时拒绝分析，避免 AI 基于主诉/诊断臆测
            val hasHistory = records.any { it.content.isNotBlank() }
            val hasVital = vital != null
            val hasImaging = imaging.isNotBlank()
            if (!hasHistory && !hasVital && !hasImaging) {
                withContext(Dispatchers.Main) {
                    resultContainer.removeAllViews()
                    addWarn("暂无病历、化验或影像报告数据。为避免 AI 基于主诉/诊断臆测病史，请先录入病历、录入体征数据，或粘贴影像报告文字后再分析。")
                }
                return@launch
            }

            localCritical = ClinicalCaseBuilder.detectCriticalValues(vital)
            val prompt = ClinicalCaseBuilder.buildPrompt(
                ClinicalCaseBuilder.CaseData(p, records, vital, imaging)
            )

            val resp = ClinicalReasoningClient.analyze(this@ClinicalAnalysisActivity, prompt)
            resp.fold(
                onSuccess = { raw ->
                    val parsed = ClinicalAnalysisResult.parse(raw)
                    // 本地危急值兜底置顶（去重）
                    val merged = parsed.copy(
                        criticalAlerts = (localCritical + parsed.criticalAlerts)
                            .distinctBy { it.item + it.value }
                    )
                    withContext(Dispatchers.Main) {
                        resultContainer.removeAllViews()
                        lastResult = merged
                        renderResult(merged)
                        if (merged.diagnoses.isNotEmpty() || merged.rawText.isNotBlank()) {
                            btnSave.visibility = View.VISIBLE
                        }
                    }
                },
                onFailure = { e ->
                    withContext(Dispatchers.Main) {
                        resultContainer.removeAllViews()
                        if (localCritical.isNotEmpty()) {
                            // 模型失败也兜底展示本地危急值
                            renderResult(ClinicalAnalysisResult(criticalAlerts = localCritical))
                            addError("模型分析失败（${e.message}），已展示本地危急值提示")
                        } else {
                            addError("分析失败：${e.message}")
                        }
                    }
                }
            )
        }
    }

    private fun renderResult(result: ClinicalAnalysisResult) {
        // 危急值（红色，置顶）
        if (result.criticalAlerts.isNotEmpty()) {
            resultContainer.addView(sectionTitle("危急值提醒", RED))
            for (a in result.criticalAlerts) {
                resultContainer.addView(alertCard(a))
            }
        }

        // 鉴别诊断
        if (result.diagnoses.isNotEmpty()) {
            resultContainer.addView(sectionTitle("鉴别诊断", TEXT_PRIMARY))
            for ((i, d) in result.diagnoses.withIndex()) {
                resultContainer.addView(diagnosisCard(i + 1, d))
            }
        }

        // 诊疗建议
        if (result.recommendations.isNotEmpty()) {
            resultContainer.addView(sectionTitle("诊疗建议", TEXT_PRIMARY))
            for (r in result.recommendations) {
                resultContainer.addView(recommendationRow(r))
            }
        }

        // 待补充
        if (result.pendingInfo.isNotEmpty()) {
            resultContainer.addView(sectionTitle("待补充信息", TEXT_SECONDARY))
            val sb = StringBuilder()
            result.pendingInfo.forEach { sb.append("· ").append(it).append("\n") }
            resultContainer.addView(TextView(this).apply {
                text = sb.toString().trim()
                textSize = 13f
                setTextColor(TEXT_SECONDARY)
                setPadding(4, 4, 4, 4)
            })
        }

        // 解析失败兜底：展示原文
        if (result.diagnoses.isEmpty() && result.recommendations.isEmpty() && result.criticalAlerts.isEmpty()) {
            resultContainer.addView(sectionTitle("原始结果", TEXT_SECONDARY))
            resultContainer.addView(TextView(this).apply {
                text = result.rawText.ifBlank { "（无结果）" }
                textSize = 13f
                setTextColor(TEXT_SECONDARY)
                setPadding(4, 4, 4, 4)
            })
        }
    }

    private fun sectionTitle(title: String, color: Int): TextView = TextView(this).apply {
        text = title
        textSize = 15f
        setTypeface(null, Typeface.BOLD)
        setTextColor(color)
        setPadding(0, 18, 0, 8)
    }

    private fun alertCard(a: ClinicalAnalysisResult.CriticalAlert): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 10)
            setBackgroundColor(0xFFFDECEA.toInt())
        }
        card.addView(TextView(this).apply {
            text = "${a.item}  ${a.value}".trim()
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(RED)
        })
        if (a.hint.isNotBlank()) {
            card.addView(TextView(this).apply {
                text = a.hint
                textSize = 12f
                setTextColor(TEXT_SECONDARY)
            })
        }
        return wrapCard(card)
    }

    private fun diagnosisCard(index: Int, d: ClinicalAnalysisResult.Diagnosis): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 10)
            setBackgroundColor(CARD_BG)
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).apply {
            text = "$index. ${d.name}"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT_PRIMARY)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (d.likelihood.isNotBlank()) {
            val lc = likelihoodColor(d.likelihood)
            head.addView(TextView(this).apply {
                text = d.likelihood
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(lc)
                setPadding(10, 3, 10, 3)
            })
        }
        card.addView(head)

        if (d.supporting.isNotEmpty()) {
            card.addView(evidenceBlock("支持证据", d.supporting, ACCENT))
        }
        if (d.against.isNotEmpty()) {
            card.addView(evidenceBlock("不支持证据", d.against, TEXT_SECONDARY))
        }
        return wrapCard(card)
    }

    private fun evidenceBlock(label: String, list: List<ClinicalAnalysisResult.Evidence>, color: Int): View {
        val block = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 6, 0, 0)
        }
        block.addView(TextView(this).apply {
            text = label
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color)
        })
        for (e in list) {
            block.addView(TextView(this).apply {
                text = buildString {
                    val tag = e.type.ifBlank { "证据" }
                    append("▸ [").append(tag).append("] ")
                    if (e.item.isNotBlank()) append(e.item)
                    if (e.value.isNotBlank()) append(" ${e.value}")
                    if (e.reference.isNotBlank()) append(" (参考 ${e.reference})")
                    if (e.note.isNotBlank()) append(" ${e.note}")
                }
                textSize = 13f
                setTextColor(TEXT_PRIMARY)
                setPadding(8, 2, 0, 2)
            })
        }
        return block
    }

    private fun recommendationRow(r: ClinicalAnalysisResult.Recommendation): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 10)
            setBackgroundColor(CARD_BG)
        }
        card.addView(TextView(this).apply {
            text = if (r.type.isNotBlank()) "[${r.type}] ${r.content}" else r.content
            textSize = 13f
            setTextColor(TEXT_PRIMARY)
        })
        if (r.basis.isNotBlank()) {
            card.addView(TextView(this).apply {
                text = "依据：${r.basis}"
                textSize = 12f
                setTextColor(TEXT_SECONDARY)
            })
        }
        return wrapCard(card)
    }

    private fun likelihoodColor(v: String): Int = when {
        v.contains("高") -> RED
        v.contains("中") -> AMBER
        v.contains("低") -> BLUE
        else -> TEXT_SECONDARY
    }

    private fun addError(msg: String) {
        resultContainer.addView(TextView(this).apply {
            text = msg
            textSize = 13f
            setTextColor(RED)
            setPadding(0, 16, 0, 0)
        })
    }

    private fun addWarn(msg: String) {
        resultContainer.addView(TextView(this).apply {
            text = msg
            textSize = 13f
            setTextColor(AMBER)
            setPadding(0, 16, 0, 0)
        })
    }

    private fun wrapCard(card: View): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 4)
        }
        wrap.addView(card)
        return wrap
    }

    private fun saveToRecord() {
        val result = lastResult ?: return
        val content = formatForRecord(result)
        lifecycleScope.launch(Dispatchers.IO) {
            db.medicalRecordDao().insert(
                MedicalRecord(
                    patientId = patientId,
                    type = "AI辅助分析",
                    content = content,
                    generatedBy = "AI"
                )
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ClinicalAnalysisActivity, "已保存为病历", Toast.LENGTH_SHORT).show()
                btnSave.visibility = View.GONE
            }
        }
    }

    private fun formatForRecord(r: ClinicalAnalysisResult): String = buildString {
        append("【AI 临床辅助分析】\n\n")
        if (r.criticalAlerts.isNotEmpty()) {
            append("危急值提醒：\n")
            r.criticalAlerts.forEach { append("⚠ ${it.item} ${it.value} ${it.hint}\n") }
            append("\n")
        }
        append("鉴别诊断：\n")
        r.diagnoses.forEachIndexed { i, d ->
            append("${i + 1}. ${d.name}（${d.likelihood}）\n")
            d.supporting.forEach { e ->
                append("   支持：${e.item} ${e.value}".trim() + "\n")
            }
            d.against.forEach { e ->
                append("   不支持：${e.item} ${e.value}".trim() + "\n")
            }
        }
        append("\n诊疗建议：\n")
        r.recommendations.forEach { r2 ->
            append("· [${r2.type}] ${r2.content}".trim() + "\n")
        }
        if (r.pendingInfo.isNotEmpty()) {
            append("\n待补充：\n")
            r.pendingInfo.forEach { append("· $it\n") }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
