package com.mynote.android.ui.patient

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import android.widget.Button
import com.mynote.android.R
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.MedicalRecord
import com.mynote.android.data.entity.Patient
import com.mynote.android.util.DeepSeekClient
import com.mynote.android.util.DeptRecordTemplate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PatientDetailActivity : AppCompatActivity() {

    companion object { private const val REQ_VOICE = 9001 }

    private var voiceTargetEt: EditText? = null
    private var patientId: Long = 0
    private lateinit var patient: Patient
    private lateinit var tvName: TextView
    private lateinit var tvInfo1: TextView
    private lateinit var tvInfo2: TextView
    private lateinit var tvDiag: TextView
    private lateinit var tvComplaint: TextView
    private lateinit var rv: RecyclerView
    private lateinit var fab: Button
    private lateinit var vitalCard: View
    private lateinit var tvVitalTemp: TextView
    private lateinit var tvVitalPulse: TextView
    private lateinit var tvVitalBp: TextView
    private lateinit var tvVitalSpo2: TextView
    private lateinit var tvVitalLab: TextView
    private val db by lazy { AppDatabase.get(this) }
    private val adapter = RecordAdapter()
    private lateinit var ocrPicker: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_detail)

        patientId = intent.getLongExtra("patient_id", 0)
        tvName = findViewById(R.id.tv_patient_name)
        tvInfo1 = findViewById(R.id.tv_info_line1)
        tvInfo2 = findViewById(R.id.tv_info_line2)
        tvDiag = findViewById(R.id.tv_info_diagnosis)
        tvComplaint = findViewById(R.id.tv_info_complaint)
        rv = findViewById(R.id.rv_records)
        fab = findViewById(R.id.fab_action)
        vitalCard = findViewById(R.id.vital_summary_card)
        tvVitalTemp = findViewById(R.id.tv_vital_temp)
        tvVitalPulse = findViewById(R.id.tv_vital_pulse)
        tvVitalBp = findViewById(R.id.tv_vital_bp)
        tvVitalSpo2 = findViewById(R.id.tv_vital_spo2)
        tvVitalLab = findViewById(R.id.tv_vital_lab)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_edit).setOnClickListener { showEditPatient() }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        adapter.onClick = { record -> showRecordDialog(record) }
        adapter.onDelete = { record -> deleteRecord(record) }

        fab.setOnClickListener { showFabMenu() }

        ocrPicker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) runMlKitOcr(uri)
        }

        loadPatient()
        loadRecords()
    }

    private fun loadPatient() {
        lifecycleScope.launch {
            patient = db.patientDao().getById(patientId) ?: return@launch
            tvName.text = patient.name
            tvInfo1.text = "${patient.age}岁  ${patient.gender}    床号: ${patient.bedNumber}    科室: ${patient.department}"
            tvInfo2.text = "入院: ${patient.admissionDate}"
            tvDiag.text = "诊断: ${patient.diagnosis}"
            tvComplaint.text = "主诉: ${patient.chiefComplaint}"
            loadVitalSummary()
        }
    }

    private fun loadVitalSummary() {
        lifecycleScope.launch(Dispatchers.IO) {
            val latest = db.vitalSignsDao().getSince(patientId, System.currentTimeMillis() - 90L * 24 * 3600 * 1000).lastOrNull()
            if (latest == null) return@launch
            withContext(Dispatchers.Main) {
                val parts = mutableListOf<String>()
                if (latest.temperature != null) tvVitalTemp.text = "T${latest.temperature}℃"
                else tvVitalTemp.text = "-"
                if (latest.pulse != null) tvVitalPulse.text = "P${latest.pulse}"
                else tvVitalPulse.text = "-"
                val bp = if (latest.bpSystolic != null && latest.bpDiastolic != null) "${latest.bpSystolic}/${latest.bpDiastolic}" else "-"
                tvVitalBp.text = "BP$bp"
                if (latest.spo2 != null) tvVitalSpo2.text = "O₂${latest.spo2}%"
                else tvVitalSpo2.text = "-"
                val labs = buildString {
                    if (latest.hba1c != null) append("HbA1c${latest.hba1c}% ")
                    if (latest.creatinine != null) append("Cr${latest.creatinine} ")
                    if (latest.egfr != null) append("eGFR${"%.0f".format(latest.egfr)} ")
                    if (latest.hemoglobin != null) append("Hb${latest.hemoglobin} ")
                }
                tvVitalLab.text = if (labs.isNotBlank()) labs.trim() else "无化验"
                vitalCard.visibility = View.VISIBLE
            }
        }
    }

    private fun loadRecords() {
        lifecycleScope.launch {
            db.medicalRecordDao().getByPatient(patientId).collectLatest {
                adapter.submitList(it)
            }
        }
    }

    private fun showEditPatient() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 0)
        }
        val edits = listOf("姓名" to patient.name, "年龄" to patient.age.toString(),
            "性别" to patient.gender, "床号" to patient.bedNumber,
            "科室" to patient.department, "入院日期" to patient.admissionDate,
            "诊断" to patient.diagnosis, "主诉" to patient.chiefComplaint).map { (h, v) ->
            EditText(this).apply { hint = h; setText(v); inputType = InputType.TYPE_CLASS_TEXT }.also { root.addView(it) }
        }
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("编辑患者")
            .setView(root)
            .setPositiveButton("保存") { _, _ ->
                val p = patient.copy(
                    name = edits[0].text.toString().trim(), age = edits[1].text.toString().toIntOrNull() ?: 0,
                    gender = edits[2].text.toString().trim(), bedNumber = edits[3].text.toString().trim(),
                    department = edits[4].text.toString().trim(), admissionDate = edits[5].text.toString().trim(),
                    diagnosis = edits[6].text.toString().trim(), chiefComplaint = edits[7].text.toString().trim()
                )
                lifecycleScope.launch { db.patientDao().update(p); loadPatient() }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showFabMenu() {
        val items = arrayOf("AI 生成病历", "手动录入病历", "识别化验单", "导出病历数据", "打印病历", "体征趋势", "随访设置", "用药日历", "清空所有病历", "删除患者")
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showAIGenerateDialog()
                    1 -> showManualRecordDialog()
                    2 -> scanLabReport()
                    3 -> {
                        val fmts = arrayOf("TXT 纯文本", "MD Markdown", "DOCX Word", "PDF 正式排版")
                        AlertDialog.Builder(this, R.style.GlassDialog)
                            .setTitle("导出格式")
                            .setItems(fmts) { _, i -> exportRecords(when(i) { 1 -> "md"; 2 -> "docx"; 3 -> "pdf"; else -> "txt" }) }
                            .show()
                    }
                    4 -> printRecordsPDF()
                    5 -> showVitalTrends()
                    6 -> showFollowupDialog()
                    7 -> showMedicationCalendar()
                    8 -> {
                        AlertDialog.Builder(this, R.style.GlassDialog)
                            .setTitle("清空病历")
                            .setMessage("删除「${patient.name}」的全部病历记录？")
                            .setPositiveButton("清空") { _, _ ->
                                lifecycleScope.launch { db.medicalRecordDao().deleteByPatient(patientId) }
                                Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show()
                            }.setNegativeButton("取消", null).show()
                    }
                    9 -> {
                        AlertDialog.Builder(this, R.style.GlassDialog)
                            .setTitle("确认删除")
                            .setMessage("删除「${patient.name}」和所有病历？")
                            .setPositiveButton("删除") { _, _ ->
                                lifecycleScope.launch { db.patientDao().delete(patient); finish() }
                            }.setNegativeButton("取消", null).show()
                    }
                }
            }.show()
    }

    private fun showManualRecordDialog() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 0) }
        val etType = EditText(this).apply { hint = "文书类型（入院记录/日常病程等）"; inputType = InputType.TYPE_CLASS_TEXT }
        val etContent = EditText(this).apply {
            hint = "病历内容"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 8; gravity = android.view.Gravity.TOP
        }
        root.addView(etType); root.addView(etContent)

        // 导入模板按钮
        val btnBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.END
            setPadding(0, dp(6), 0, 0)
        }
        val btnImport = TextView(this).apply {
            text = "导入模板"
            textSize = 13f; setTextColor(Color.parseColor("#616161"))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            val out = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, out, true)
            setBackgroundResource(out.resourceId)
            setOnClickListener {
                showTemplatePicker { t -> etContent.setText(t) }
            }
        }
        btnBar.addView(btnImport)
        root.addView(btnBar)

        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("录入病历")
            .setView(root)
            .setPositiveButton("保存") { _, _ ->
                val rec = MedicalRecord(patientId = patientId, type = etType.text.toString().trim(),
                    content = etContent.text.toString().trim(), generatedBy = "手动")
                lifecycleScope.launch { db.medicalRecordDao().insert(rec) }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 模板选择器（通用+专科），选中回调 content */
    private fun showTemplatePicker(onPicked: (String) -> Unit) {
        val deptTemps = com.mynote.android.util.DeptTemplates.getAll()
        val deptGroups = deptTemps.groupBy { it.title.substringBefore("-") }
        val cats = mutableListOf("通用模板")
        cats.addAll(deptGroups.keys.map { "${it}·专科" })

        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("选择模板")
            .setItems(cats.toTypedArray()) { _, ci ->
                if (ci == 0) {
                    val temps = com.mynote.android.util.TemplateManager.getAll()
                    val items = temps.map { it.title }.toTypedArray()
                    AlertDialog.Builder(this, R.style.GlassDialog).setTitle("通用模板")
                        .setItems(items) { _, i -> onPicked(temps[i].content) }
                        .setNegativeButton("返回") { _, _ -> showTemplatePicker(onPicked) }.show()
                } else {
                    val dept = cats[ci].removeSuffix("·专科")
                    val group = deptGroups[dept] ?: return@setItems
                    val items = group.map { it.title }.toTypedArray()
                    AlertDialog.Builder(this, R.style.GlassDialog).setTitle("${dept}·专科")
                        .setItems(items) { _, i -> onPicked(group[i].content) }
                        .setNegativeButton("返回") { _, _ -> showTemplatePicker(onPicked) }.show()
                }
            }.setNegativeButton("取消", null).show()
    }

    private fun showRecordDialog(record: MedicalRecord) {
        val ctx = this
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 16, 24, dp(6)) }
        val scroll = ScrollView(this).apply { addView(TextView(ctx).apply {
            text = highlightPending(record.content); textSize = 14f; setPadding(8, 0, 8, 0)
            setTextColor(resources.getColor(R.color.text_primary, null))
            setTextIsSelectable(true)
        })}
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // 顶部分隔线
        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        root.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1).apply {
            topMargin = dp(8)
        })

        val btns = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))
        }
        root.addView(btns)

        val score = com.mynote.android.util.RecordScorer.score(record.type, record.content)
        val dialog = AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("${record.type}  ${if(record.generatedBy == "AI") "AI" else "手动"}  ${com.mynote.android.util.RecordScorer.starLabel(score.total)}")
            .setView(root)
            .setCancelable(true)
            .create()

        val btnColor = Color.parseColor("#616161") // Material Gray 600 — 柔和深灰
        for ((text, action) in listOf(
            "编辑" to { dialog.dismiss(); editRecord(record) },
            "补全" to { dialog.dismiss(); fillPending(record) },
            "润色" to { dialog.dismiss(); showRewriteDialog(record) },
            "审校" to { dialog.dismiss(); showAuditDialog(record) },
            "复制" to { dialog.dismiss(); copyRecord(record) },
            "关闭" to { dialog.dismiss() }
        )) {
            val btn = TextView(ctx).apply {
                setText(text)
                textSize = 13f
                setTextColor(btnColor)
                gravity = android.view.Gravity.CENTER
                setPadding(dp(4), dp(10), dp(4), dp(10))
                // Material ripple
                val out = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, out, true)
                setBackgroundResource(out.resourceId)
                setOnClickListener { action() }
            }
            btns.addView(btn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun editRecord(record: MedicalRecord) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 0) }
        val et = EditText(this).apply {
            setText(record.content); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 12; gravity = android.view.Gravity.TOP; textSize = 14f
        }
        root.addView(et)
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("编辑 ${record.type}")
            .setView(root)
            .setPositiveButton("保存") { _, _ ->
                lifecycleScope.launch {
                    db.medicalRecordDao().update(record.copy(content = et.text.toString().trim()))
                    Toast.makeText(this@PatientDetailActivity, "已保存", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null).show()
    }

    private fun fillPending(record: MedicalRecord) {
        val pendingPats = listOf("【待补充[^】]*】", "（待补充[^）]*）", "\\(待补充[^)]*\\)", "\\[待补充[^\\]]*\\]")
        val pending = pendingPats.flatMap { pat -> Regex(pat).findAll(record.content).toList() }
            .sortedBy { it.range.first }
        if (pending.isEmpty()) { Toast.makeText(this, "无待补充内容", Toast.LENGTH_SHORT).show(); return }

        val scrollView = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 16, 40, 0) }
        val fields = mutableListOf<Pair<IntRange, EditText>>()

        for (m in pending) {
            val label = extractSectionLabel(record.content, m.range.first)
            val ctx = extractContext(record.content, m.range.first)
            // 标签 = 段落标题 + 上下文片段（区分同段多个待补充）
            val title = if (ctx.isNotEmpty()) "$label · $ctx" else label
            val et = EditText(this).apply {
                hint = "填写：$label"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 2; gravity = android.view.Gravity.TOP
                setBackgroundResource(android.R.drawable.edit_text)
                setPadding(16, 10, 16, 10)
                // 智能预填：上下文匹配患者信息字段
                val prefill = matchPatientField(ctx)
                if (prefill != null) setText(prefill)
            }
            fields.add(m.range to et)
            root.addView(TextView(this).apply { text = title; textSize = 13f; setTypeface(null, Typeface.BOLD); setPadding(0, 10, 0, 2) })
            // 语音输入行
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(et, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val mic = TextView(this).apply {
                text = "语音"; textSize = 12f
                setTextColor(Color.parseColor("#1565C0"))
                setTypeface(null, Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setBackgroundResource(android.R.drawable.btn_default_small)
                setOnClickListener { launchVoiceInput(et) }
            }
            row.addView(mic)
            root.addView(row)
        }
        scrollView.addView(root)

        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("补全待补充内容（${pending.size}项）")
            .setView(scrollView)
            .setPositiveButton("回填") { _, _ ->
                val sb = StringBuilder(record.content)
                var offset = 0
                for ((range, et) in fields) {
                    val txt = et.text.toString().trim()
                    val replacement = if (txt.isNotEmpty()) txt else "【待补充】"
                    val origStart = range.first - offset
                    val origEnd = range.last + 1 - offset
                    sb.replace(origStart, origEnd, replacement)
                    offset += (range.last + 1 - range.first) - replacement.length
                }
                lifecycleScope.launch {
                    db.medicalRecordDao().update(record.copy(content = sb.toString().trim()))
                    Toast.makeText(this@PatientDetailActivity, "已回填", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 找到【待补充】所属的段落标题（向上查找最近的标题关键词） */
    private fun extractSectionLabel(text: String, pos: Int): String {
        val before = text.substring(0, pos.coerceAtMost(text.length))
        val headers = listOf("主诉", "现病史", "既往史", "个人史", "婚育史", "家族史",
            "体格检查", "辅助检查", "初步诊断", "诊疗计划", "用药史", "过敏史", "专科检查",
            "实验室检查", "影像学检查", "治疗经过", "手术经过", "一般项目")
        var bestHeader = "待补充"
        var bestIdx = -1
        for (h in headers) {
            val idx = before.lastIndexOf(h)
            // 只取位置最近的标题，且必须在 500 字符窗口内
            if (idx >= 0 && idx > bestIdx && idx > pos - 500) {
                bestIdx = idx
                bestHeader = h
            }
        }
        return bestHeader
    }

    /** 提取【待补充】前的文字片段作为上下文（取最近一行/30字，用于区分同段多个待补充） */
    private fun extractContext(text: String, pos: Int): String {
        val before = text.substring(0, pos.coerceAtMost(text.length))
        // 取最近一行（从上一个换行开始）
        val lineStart = before.lastIndexOf('\n').let { if (it < 0) 0 else it + 1 }
        val line = before.substring(lineStart).trim()
        // 截取末尾最多 30 字
        val snippet = if (line.length > 30) "…${line.takeLast(30)}" else line
        return snippet
    }

    /** 根据上下文字段名匹配患者信息，用于智能预填 */
    private fun matchPatientField(context: String): String? {
        val c = context.replace("□", "").replace("：", "").replace(":", "").trim()
        val p = patient
        return when {
            c == "姓名" || c.startsWith("姓名") -> p.name.takeIf { it.isNotBlank() }
            c == "年龄" || c.startsWith("年龄") -> p.age.takeIf { it > 0 }?.toString()
            c == "性别" || c.startsWith("性别") -> p.gender.takeIf { it.isNotBlank() }
            c == "床号" || c.startsWith("床号") -> p.bedNumber.takeIf { it.isNotBlank() }
            c == "科室" || c.startsWith("科室") -> p.department.takeIf { it.isNotBlank() }
            c == "入院日期" || c.startsWith("入院日期") -> p.admissionDate.takeIf { it.isNotBlank() }
            c == "诊断" || c == "入院诊断" || c == "初步诊断" -> p.diagnosis.takeIf { it.isNotBlank() }
            (c == "主诉" || c.startsWith("主诉")) && !c.contains("现病史") -> p.chiefComplaint.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    private fun copyRecord(record: MedicalRecord) {
        val clip = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clip.setPrimaryClip(android.content.ClipData.newPlainText("record", record.content))
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
    }

    // ===== AI 润色改写 =====

    private fun showRewriteDialog(record: MedicalRecord) {
        val actions = listOf(
            Triple("展开鉴别诊断",
                "请审阅以下病历的鉴别诊断部分。如果已有鉴别诊断但不够详尽（每条不足50字、缺少排除依据），请补充完善。如果完全缺失鉴别诊断段落，请根据诊断和症状新增完整的鉴别诊断。每个鉴别疾病写明：相似表现、排除该病的关键依据。如果原文鉴别诊断已经非常充分，则保持原文不变。",
                "鉴别诊断"),
            Triple("补充用药方案",
                "请审阅以下病历的用药相关部分。如果用药方案缺少剂量、用法、频次或疗程等细节，请补充完善（通用名+剂量+给药途径+频次+疗程）。如果已有完整用药方案则保持原文不变。",
                "用药方案"),
            Triple("精简为要点",
                "请将以下病历精简为核心要点列表，每个要点一行用「•」开头。只保留关键信息，删去冗余描述。",
                "要点"),
            Triple("润色文书格式",
                "请审阅以下病历的格式和用语。如果存在不规范术语、口语化表达、标点格式不统一等问题，请修正。如果格式已规范，保持原文不变。",
                "文书格式"),
            Triple("完善体格检查",
                "请审阅以下病历的体格检查部分。如果体格检查过于简略（缺少系统描述、检查项目<5项），请按系统补充完整（一般情况→皮肤→淋巴结→头颈→胸→腹→脊柱四肢→神经）。如果已有完整的逐系统查体记录，保持原文不变。",
                "体格检查")
        )
        val items = actions.map { it.first }.toTypedArray()
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("AI 润色 · ${record.type}")
            .setItems(items) { _, i ->
                val (title, instruction, resultLabel) = actions[i]
                doRewrite(record, instruction, resultLabel)
            }.setNegativeButton("取消", null).show()
    }

    private fun doRewrite(record: MedicalRecord, instruction: String, resultLabel: String) {
        val prompt = "你是三甲医院主任医师。以下是病历原文，请根据要求进行审阅和必要补充。已有充分内容的部分保持不动，只对不足的部分进行补充完善。\n\n【原文】\n${record.content}\n\n【要求】\n$instruction\n\n直接输出完整病历，不要加解释。"

        val params = DeepSeekClient.GenParams(
            patientName = patient.name, age = patient.age, gender = patient.gender,
            bedNumber = patient.bedNumber, department = patient.department,
            admissionDate = patient.admissionDate, diagnosis = patient.diagnosis,
            chiefComplaint = patient.chiefComplaint,
            recordTypes = listOf(record.type)
        )

        streamGen(params, saveLabel = "查看差异", customPrompt = prompt,
            dialogTitle = "AI 润色 · $resultLabel",
            afterStream = { dialog, rewritten ->
                val (diff, newCount) = highlightDiff(record.content, rewritten)
                dialog.setTitle("润色差异 · $resultLabel")
                // 最安全的方式：直接改 streamTv 的文本
                val tv = streamTv
                if (tv != null) {
                    tv.text = if (newCount > 0) "➕ = 新增/改动（共 $newCount 处）\n\n$diff"
                              else "✅ 原文已完善，未新增内容\n\n$diff"
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.text = "保存"
            }
        ) { rewritten ->
            lifecycleScope.launch {
                db.medicalRecordDao().insert(
                    MedicalRecord(patientId = patientId, type = "${record.type}（$resultLabel）",
                        content = rewritten, generatedBy = "AI")
                )
                Toast.makeText(this@PatientDetailActivity, "已保存", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 行级 diff：返回(标注文本, 新增行数)。新增行前加 ➕ 前缀 */
    private fun highlightDiff(original: String, rewritten: String): Pair<String, Int> {
        val origLines = original.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val rewriterLines = rewritten.split("\n")
        val sb = StringBuilder()
        var newCount = 0
        for (line in rewriterLines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.matches(Regex("^[═=—\\-]+$"))) {
                sb.append(line).append("\n"); continue
            }
            val hasMatch = trimmed.length <= 5 || origLines.any { orig ->
                trimmed == orig || (trimmed.length > 6 && orig.length > 6 &&
                    (orig.contains(trimmed.take(20)) || trimmed.contains(orig.take(20))))
            }
            if (!hasMatch) {
                sb.append("➕ ").append(line).append("\n")
                newCount++
            } else {
                sb.append(line).append("\n")
            }
        }
        return sb.toString() to newCount
    }

    private fun escHtml(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /** 润色预览 */
    private fun showDiffPreview(record: MedicalRecord, rewritten: String, diff: SpannableString, label: String, newCount: Int) {
        val scroll = ScrollView(this)
        val tv = TextView(this).apply {
            text = diff; textSize = 13f; setPadding(32, 20, 32, 20)
        }
        val hint = TextView(this).apply {
            text = if (newCount > 0) "➕ = 新增/改动（共 $newCount 处）"
                   else "✅ 原文已完善，未新增内容（可直接保存或放弃）"
            textSize = 12f; setTextColor(Color.parseColor("#616161")); setPadding(32, 0, 32, 8)
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(hint); root.addView(scroll)

        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("润色结果 · ${record.type}（$label）")
            .setView(root)
            .setPositiveButton("保存") { _, _ ->
                lifecycleScope.launch {
                    db.medicalRecordDao().insert(
                        MedicalRecord(patientId = patientId, type = "${record.type}（$label）",
                            content = rewritten, generatedBy = "AI")
                    )
                    Toast.makeText(this@PatientDetailActivity, "已保存", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("放弃", null)
            .show()
    }

    // ===== AI 病历审校 =====

    private fun showAuditDialog(record: MedicalRecord) {
        val issues = auditRecord(record.content)
        if (issues.isEmpty()) {
            AlertDialog.Builder(this, R.style.GlassDialog)
                .setTitle("审校结果")
                .setMessage("✅ 未发现明显问题")
                .setPositiveButton("好的", null).show()
            return
        }
        val sb = StringBuilder()
        var warnCount = 0; var errCount = 0
        for ((severity, msg) in issues) {
            val icon = if (severity == "error") { errCount++; "❌" } else { warnCount++; "⚠️" }
            sb.append("$icon $msg\n")
        }
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("审校结果（${errCount}个错误 ${warnCount}个提醒）")
            .setMessage(sb.toString())
            .setPositiveButton("关闭", null)
            .show()
    }

    /** 病历审校规则引擎 */
    private fun auditRecord(text: String): List<Pair<String, String>> {
        val issues = mutableListOf<Pair<String, String>>() // (severity, message)

        // 1. 必填段落检查
        val required = mapOf(
            "主诉" to "缺少主诉段落",
            "现病史" to "缺少现病史段落",
            "既往史" to "缺少既往史段落",
            "体格检查" to "缺少体格检查段落",
            "初步诊断" to "缺少初步诊断段落",
            "诊疗计划" to "缺少诊疗计划段落"
        )
        for ((kw, msg) in required) {
            if (!text.contains(kw)) issues.add("error" to msg)
        }

        // 2. 仍存在【待补充】标记
        val pendingCount = Regex("【待补充[^】]*】").findAll(text).count()
        if (pendingCount > 0) issues.add("warn" to "仍存在${pendingCount}处【待补充】需填写")

        // 3. 药物缺少剂量单位（常见模式：药名后无 mg/g/mL/片/袋/支 等）
        val drugPatterns = listOf(
            Regex("阿司匹林(?!\\s*\\d+\\s*(mg|g|片))"),
            Regex("氯吡格雷(?!\\s*\\d+\\s*(mg|片))"),
            Regex("替格瑞洛(?!\\s*\\d+\\s*(mg|片))"),
            Regex("甲泼尼龙(?!\\s*\\d+\\s*(mg|g))"),
            Regex("呋塞米(?!\\s*\\d+\\s*(mg))"),
            Regex("氨氯地平(?!\\s*\\d+\\s*(mg))"),
            Regex("美托洛尔(?!\\s*\\d+\\s*(mg))"),
            Regex("阿托伐他汀(?!\\s*\\d+\\s*(mg))")
        )
        var missingDosage = 0
        for (pat in drugPatterns) {
            if (pat.containsMatchIn(text)) missingDosage++
        }
        if (missingDosage > 0) issues.add("warn" to "${missingDosage}处用药可能缺少剂量/单位")

        // 4. 诊断格式检查：诊断后应有编号或明确分行
        if (text.contains("初步诊断") && !Regex("初步诊断[^。]*\\d+[.、)]").containsMatchIn(text)) {
            issues.add("warn" to "初步诊断建议编号排列（1. 2. 3.）")
        }

        // 5. 生命体征不完整
        if (text.contains("体格检查") && !Regex("T\\s*[\\d.]+℃").containsMatchIn(text)) {
            issues.add("warn" to "体格检查缺少体温记录（T__℃）")
        }
        if (text.contains("体格检查") && !Regex("P\\s*\\d+次/分").containsMatchIn(text)) {
            issues.add("warn" to "体格检查缺少脉搏记录（P__次/分）")
        }
        if (text.contains("体格检查") && !Regex("BP\\s*\\d+/\\d+").containsMatchIn(text)) {
            issues.add("warn" to "体格检查缺少血压记录（BP__/__mmHg）")
        }

        // 6. 鉴别诊断缺失（首次病程必须包含）
        if (text.contains("首次病程") && !text.contains("鉴别诊断")) {
            issues.add("warn" to "首次病程记录缺少鉴别诊断部分")
        }

        // 7. 诊断缩写或不规范
        val abbreviations = mapOf(
            "高血压病" to "建议使用规范诊断名（高血压病→原发性高血压 __级 危险分层__）",
            "冠心病" to "建议使用规范诊断名（冠心病→冠状动脉粥样硬化性心脏病）",
            "糖尿病" to "建议使用规范诊断名（糖尿病→2型糖尿病/1型糖尿病）"
        )
        for ((abbr, msg) in abbreviations) {
            if (text.contains(abbr) && !text.contains(msg.substringBefore("→"))) {
                // only warn once
            }
        }

        return issues
    }

    private fun deleteRecord(record: MedicalRecord) {
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("删除病历")
            .setMessage("删除这条${record.type}？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch { db.medicalRecordDao().delete(record) }
            }.setNegativeButton("取消", null).show()
    }

    // ===== AI 生成 =====

    private val recordTypeOptions = listOf(
        "入院记录", "首次病程记录", "日常病程记录", "上级查房记录",
        "会诊记录", "出院小结", "抢救记录", "术前小结", "手术记录", "死亡讨论"
    )
    private val selectedTypes = mutableSetOf<String>()

    /** 科室专属模板匹配时，显示为"心内科·入院记录"等 */
    private fun getDisplayLabels(): List<String> {
        val dept = patient.department
        return if (DeptRecordTemplate.hasSpecialty(dept)) {
            recordTypeOptions.map { "$dept·$it" }
        } else {
            recordTypeOptions
        }
    }

    private fun showAIGenerateDialog() {
        selectedTypes.clear()
        val labels = getDisplayLabels()
        val dept = patient.department
        val hasSpec = DeptRecordTemplate.hasSpecialty(dept)
        val title = if (hasSpec) "选择文书类型（${dept}专科）" else "选择文书类型"

        // 加载收藏
        val favs = loadFavorites()

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 8, 0, 8) }

        // 收藏区域
        if (favs.isNotEmpty()) {
            val favSection = TextView(this).apply {
                text = "⭐ 收藏"; textSize = 13f; setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#333333")); setPadding(24, 0, 0, 4)
            }
            root.addView(favSection)
            for (fav in favs) {
                val btn = TextView(this).apply {
                    text = fav; textSize = 13f; setPadding(24, 10, 24, 10)
                    setTextColor(Color.parseColor("#1565C0"))
                    val out = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
                    setBackgroundResource(out.resourceId)
                    setOnClickListener {
                        val types = fav.split("+").map { it.trim() }
                        for (t in types) selectedTypes.add(t)
                        Toast.makeText(this@PatientDetailActivity, "已选：$fav", Toast.LENGTH_SHORT).show()
                    }
                }
                root.addView(btn)
            }
            val sep = View(this).apply {
                setBackgroundColor(Color.parseColor("#E0E0E0")); layoutParams =
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1).apply { setMargins(16, 6, 16, 6) }
            }
            root.addView(sep)
        }

        // 全部类型
        root.addView(TextView(this).apply {
            text = "全部类型"; textSize = 13f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#333333")); setPadding(24, 0, 0, 4)
        })
        val checks = mutableListOf<android.widget.CheckBox>()
        for (la in labels) {
            val cb = android.widget.CheckBox(this).apply {
                text = la; textSize = 13f; setPadding(24, 8, 24, 8)
                val out = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
                setBackgroundResource(out.resourceId)
                setOnCheckedChangeListener { _, _ ->
                    selectedTypes.clear()
                    checks.forEachIndexed { j, c ->
                        if (c.isChecked) selectedTypes.add(recordTypeOptions[j])
                    }
                }
            }
            checks.add(cb)
            root.addView(cb)
        }
        scroll.addView(root)

        val dialog = AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle(title)
            .setView(scroll)
            .setPositiveButton("下一步") { _, _ ->
                if (selectedTypes.isEmpty()) {
                    Toast.makeText(this, "请至少选一种", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // 询问是否收藏
                if (favs.none { it == selectedTypes.joinToString(" + ") }) {
                    AlertDialog.Builder(this, R.style.GlassDialog)
                        .setTitle("收藏此组合？")
                        .setMessage("保存「${selectedTypes.joinToString(" + ")}」为快捷收藏，下次一键选择。")
                        .setPositiveButton("收藏") { _, _ ->
                        saveFavorite(selectedTypes.joinToString(" + "))
                        showAIDataInput()
                    }
                        .setNegativeButton("不用") { _, _ -> showAIDataInput() }
                        .show()
                } else {
                    showAIDataInput()
                }
            }
            .setNegativeButton("取消", null)
        dialog.show()
    }

    private fun loadFavorites(): List<String> {
        val raw = getSharedPreferences("ai_prefs", MODE_PRIVATE)
            .getString("ai_record_favorites", "") ?: ""
        return raw.split("|||").filter { it.isNotBlank() }
    }

    private fun saveFavorite(combo: String) {
        val favs = loadFavorites().toMutableList()
        if (!favs.contains(combo)) {
            favs.add(combo)
            getSharedPreferences("ai_prefs", MODE_PRIVATE)
                .edit().putString("ai_record_favorites", favs.joinToString("|||")).apply()
            Toast.makeText(this, "已收藏", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAIDataInput() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(40, 16, 40, 0)
        }
        val etHistory = EditText(this).apply { hint = "现病史（可选）"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; minLines = 3 }
        val etExam = EditText(this).apply { hint = "查体（可选）"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; minLines = 3 }
        val etLab = EditText(this).apply { hint = "检验/检查结果（可选）"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; minLines = 3 }
        val etOrders = EditText(this).apply { hint = "住院医嘱（可选）"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; minLines = 3 }
        root.addView(etHistory); root.addView(etExam); root.addView(etLab); root.addView(etOrders)

        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("补充临床信息（可选）")
            .setView(root)
            .setPositiveButton("开始生成") { _, _ ->
                val base = DeepSeekClient.GenParams(
                    patientName = patient.name, age = patient.age, gender = patient.gender,
                    bedNumber = patient.bedNumber, department = patient.department,
                    admissionDate = patient.admissionDate, diagnosis = patient.diagnosis,
                    chiefComplaint = patient.chiefComplaint,
                    presentIllness = etHistory.text.toString().trim(),
                    physicalExam = etExam.text.toString().trim(),
                    labResults = etLab.text.toString().trim(),
                    orders = etOrders.text.toString().trim(),
                    recordTypes = selectedTypes.toList()
                )
                if (selectedTypes.size <= 1) {
                    doGenerateSingle(base)
                } else {
                    currentGenParams = base
                    pendingTypes.clear()
                    generatedTexts.clear()
                    pendingTypes.addAll(selectedTypes)
                    doGenerateNext()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private var streamDialog: AlertDialog? = null
    private var streamTv: TextView? = null
    private var streamScroll: ScrollView? = null

    // 分步生成队列
    private val pendingTypes = mutableListOf<String>()
    private val generatedTexts = mutableListOf<Pair<String, String>>()
    private var currentGenParams: DeepSeekClient.GenParams? = null

    /** 单选直接生成并保存 */
    private fun doGenerateSingle(params: DeepSeekClient.GenParams) {
        val singleType = params.recordTypes.firstOrNull() ?: "AI生成"
        streamGen(params) { fullText ->
            saveGeneratedRecords(fullText, singleType)
        }
    }

    /** 多选分步生成队列 */
    private fun doGenerateNext() {
        if (pendingTypes.isEmpty()) {
            // 全部生成完毕，逐个保存
            val types = generatedTexts.map { it.first }
            val contents = generatedTexts.map { it.second }
            lifecycleScope.launch {
                for (i in types.indices) {
                    db.medicalRecordDao().insert(MedicalRecord(
                        patientId = patientId, type = types[i], content = contents[i], generatedBy = "AI"
                    ))
                }
                Toast.makeText(this@PatientDetailActivity, "全部已保存", Toast.LENGTH_SHORT).show()
            }
            streamDialog?.dismiss(); streamDialog = null
            return
        }
        val nextType = pendingTypes.removeAt(0)
        val params = currentGenParams!!.copy(recordTypes = listOf(nextType))
        streamDialog?.dismiss(); streamDialog = null

        val nextLabel = pendingTypes.firstOrNull()?.let { "保存并继续 → $it" }
        streamGen(params, saveLabel = nextLabel) { fullText ->
            generatedTexts.add(nextType to fullText)
            doGenerateNext()
        }
    }

    /** 核心流式生成方法。afterStream 为润色后原地展示差异的回调 */
    private fun streamGen(params: DeepSeekClient.GenParams, saveLabel: String? = null,
                          customPrompt: String? = null, dialogTitle: String = "AI 病历生成中",
                          afterStream: ((AlertDialog, String) -> Unit)? = null,
                          onDone: (String) -> Unit) {
        val label = saveLabel ?: "保存病历"
        val scroll = ScrollView(this)
        val tv = TextView(this).apply {
            text = "⏳ 正在生成 ${params.recordTypes.firstOrNull() ?: ""}...\n"; textSize = 13f
            setPadding(32, 20, 32, 20)
            setTextColor(resources.getColor(R.color.text_primary, null))
        }
        streamTv = tv; scroll.addView(tv)
        streamScroll = scroll

        val dialog = AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle(dialogTitle)
            .setView(scroll)
            .setPositiveButton(label, null)
            .setNeutralButton("继续改写") { _, _ -> /* 生成完成后激活 */ }
            .setNegativeButton("取消") { _, _ -> streamDialog = null; pendingTypes.clear() }
            .setCancelable(false)
            .create()
        streamDialog = dialog; dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.visibility = View.GONE
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.visibility = View.GONE

        val callback = object : DeepSeekClient.StreamCallback {
            override fun onToken(token: String) { tv.post { tv.append(token) } }
            override fun onDone(fullText: String) { var currentText = fullText
                tv.post {
                    tv.append("\n\n✅ 生成完成")
                    val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    btn?.visibility = View.VISIBLE
                    // 继续改写按钮
                    val btnRewrite = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    btnRewrite?.visibility = View.VISIBLE
                    btnRewrite?.setOnClickListener {
                        showContinueRewrite(dialog, currentText, params, afterStream, onDone)
                    }
                    if (afterStream != null) {
                        var step = 0
                        btn?.setOnClickListener {
                            if (step == 0) {
                                tv.text = "🔍 差异对比中..."
                                afterStream(dialog, fullText)
                                step = 1
                            } else {
                                dialog.dismiss(); streamDialog = null
                                onDone(currentText)
                            }
                        }
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                            text = "放弃"
                            setOnClickListener { dialog.dismiss(); streamDialog = null }
                        }
                    } else {
                        btn?.setOnClickListener {
                            dialog.dismiss(); streamDialog = null
                            onDone(currentText)
                        }
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.text = "关闭"
                    }
                }
            }
            override fun onError(e: Exception) {
                tv.post {
                    tv.append("\n\n❌ ${e.message}")
                    val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    btn?.visibility = View.VISIBLE
                    btn?.text = "重试"
                    btn?.setOnClickListener {
                        dialog.dismiss(); streamDialog = null
                        streamGen(params, saveLabel, customPrompt, dialogTitle, afterStream, onDone)
                    }
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.text = "关闭"
                }
            }
        }
        if (customPrompt != null) {
            DeepSeekClient.generateStreamWithPrompt(this, customPrompt, callback)
        } else {
            DeepSeekClient.generateStream(this, params, callback)
        }
    }

    private fun showGeneratedResult(rawText: String) {
        val scroll = ScrollView(this)
        val tv = TextView(this).apply {
            text = rawText; textSize = 13f; setPadding(32, 20, 32, 20)
            setTextColor(resources.getColor(R.color.text_primary, null))
        }
        scroll.addView(tv)

        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("生成结果")
            .setView(scroll)
            .setPositiveButton("保存病历") { _, _ -> saveGeneratedRecords(rawText) }
            .setNegativeButton("放弃", null)
            .show()
    }

    private fun saveGeneratedRecords(rawText: String, fallbackType: String? = null) {
        val regex = Regex("【(.+?)】")
        // 只保留实际文书类型标记，过滤【待补充：xxx】等占位符
        val matches = regex.findAll(rawText).filter {
            val type = it.groupValues[1]
            recordTypeOptions.contains(type) || type in listOf("入院记录", "首次病程记录", "日常病程记录",
                "上级查房记录", "会诊记录", "出院小结", "抢救记录", "术前小结", "手术记录", "死亡讨论",
                "主治查房记录", "主任查房记录", "转科记录", "阶段小结")
        }.toList()

        val typeLabel = fallbackType ?: "AI生成"
        if (matches.isEmpty()) {
            lifecycleScope.launch {
                val rid = db.medicalRecordDao().insert(MedicalRecord(
                    patientId = patientId, type = typeLabel, content = rawText, generatedBy = "AI"
                ))
                parseAndSaveVitals(rid, rawText)
            }
            return
        }

        lifecycleScope.launch {
            for (i in matches.indices) {
                val type = matches[i].groupValues[1]
                val start = matches[i].range.last + 1
                val end = if (i + 1 < matches.size) matches[i + 1].range.first else rawText.length
                val content = rawText.substring(start, end).trim().trimStart('-').trim()
                if (content.isNotEmpty()) {
                    val rid = db.medicalRecordDao().insert(MedicalRecord(
                        patientId = patientId, type = type, content = content, generatedBy = "AI"
                    ))
                    parseAndSaveVitals(rid, content)
                }
            }
        }
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
    }

    private fun parseAndSaveVitals(recordId: Long, content: String) {
        val v = com.mynote.android.util.VitalSignsParser.parse(patientId, recordId, content)
        if (v != null) lifecycleScope.launch { db.vitalSignsDao().insert(v) }
    }

    private fun showVitalTrends() {
        lifecycleScope.launch {
            val vitals = db.vitalSignsDao().getSince(patientId, System.currentTimeMillis() - 90L * 24 * 3600 * 1000)
            if (vitals.isEmpty()) { Toast.makeText(this@PatientDetailActivity, "暂无结构化体征数据", Toast.LENGTH_SHORT).show(); return@launch }
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            val sb = StringBuilder()
            for (v in vitals) {
                sb.append("📅 ${sdf.format(Date(v.recordedAt))}: ")
                val p = mutableListOf<String>()
                if (v.temperature != null) p.add("T${v.temperature}℃")
                if (v.pulse != null) p.add("P${v.pulse}")
                if (v.bpSystolic != null) p.add("BP${v.bpSystolic}/${v.bpDiastolic}")
                if (v.spo2 != null) p.add("SpO₂${v.spo2}%")
                if (v.hba1c != null) p.add("HbA1c${v.hba1c}%")
                if (v.creatinine != null) p.add("Cr${v.creatinine}")
                if (v.egfr != null) p.add("eGFR%.0f".format(v.egfr))
                if (v.hemoglobin != null) p.add("Hb${v.hemoglobin}")
                sb.append(p.joinToString(" | ")).append("\n")
            }
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@PatientDetailActivity, R.style.GlassDialog)
                    .setTitle("体征趋势（近90天）")
                    .setMessage(sb.toString())
                    .setPositiveButton("关闭", null).show()
            }
        }
    }

    private fun scanLabReport() {
        // 从相册选照片，避免相机 FileProvider 授权问题
        ocrPicker.launch("image/*")
    }

    private fun doPatientQwenCalibrate(bmp: android.graphics.Bitmap, rawText: String, pid: Long) {
        lifecycleScope.launch {
            try {
                val corrected = com.mynote.android.util.QwenOcrClient.calibrate(this@PatientDetailActivity, bmp, rawText)
                if (corrected != null && corrected.isNotBlank()) {
                    val cleaned = com.mynote.android.util.LabReportOcr.correctOcrTypos(corrected)
                    val parsed = com.mynote.android.util.LabReportOcr.parseResults(cleaned)
                    val displayText = if (parsed.isNotEmpty()) {
                        com.mynote.android.util.LabReportOcr.formatResults(parsed)
                    } else cleaned
                    val content = displayText + "\n\n── AI 校准 ──"
                    lifecycleScope.launch {
                        db.medicalRecordDao().insert(MedicalRecord(patientId = pid, type = "化验单(AI校准)", content = content))
                        loadRecords()
                    }
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@PatientDetailActivity, R.style.GlassDialog)
                            .setTitle("AI 校准完成")
                            .setMessage(displayText)
                            .setPositiveButton("保存") { _, _ ->
                                Toast.makeText(this@PatientDetailActivity, "已保存 AI 校准结果", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("关闭", null).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val p = com.mynote.android.util.Prefs(this@PatientDetailActivity)
                        if (p.qwenApiKey.isEmpty()) {
                            AlertDialog.Builder(this@PatientDetailActivity, R.style.GlassDialog)
                                .setTitle("未配置 Qwen API")
                                .setMessage("请先在设置→AI配置中填写阿里云(Qwen) API Key")
                                .setPositiveButton("确定", null).show()
                        } else {
                            Toast.makeText(this@PatientDetailActivity, "校准失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@PatientDetailActivity, "校准异常: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun runMlKitOcr(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 同时加载图片供 Qwen 校准用
                val ocrBmp = runCatching { android.graphics.BitmapFactory.decodeStream(contentResolver.openInputStream(uri)) }.getOrNull()
                val inputImage = com.google.mlkit.vision.common.InputImage.fromFilePath(this@PatientDetailActivity, uri)
                val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                    com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions.Builder().build())
                val visionText = com.google.android.gms.tasks.Tasks.await(recognizer.process(inputImage))
                // 元素级 + 像素坐标重建：同 Y 坐标归一行，左→右排列
                val elements = mutableListOf<Triple<Float, Float, String>>() // (y, x, text)
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            val box = element.boundingBox ?: continue
                            elements.add(Triple(box.centerY().toFloat(), box.centerX().toFloat(), element.text.trim()))
                        }
                    }
                }
                elements.sortWith(compareBy({ it.first }, { it.second })) // 先 Y 后 X
                val rawText = buildString {
                    var lastY = -1f
                    for ((y, _, text) in elements) {
                        if (lastY >= 0 && (y - lastY) > 12f) append("\n") // Y 跳变 = 新行
                        else if (lastY >= 0) append(" ")
                        append(text)
                        lastY = y
                    }
                }
                if (rawText.isBlank()) {
                    withContext(Dispatchers.Main) { Toast.makeText(this@PatientDetailActivity, "未识别到文字", Toast.LENGTH_SHORT).show() }
                    return@launch
                }
                // OCR 错别字纠正
                val correctedText = com.mynote.android.util.LabReportOcr.correctOcrTypos(rawText)
                var v = com.mynote.android.util.VitalSignsParser.parse(patientId, 0, correctedText)
                val (vs, parsed) = parseLabLoose(correctedText) // 始终跑一次，拿到完整解析
                if (v == null) v = vs
                val summary = if (parsed.isNotBlank()) parsed + "\n\n── 原始文字 ──\n" + correctedText.take(800) else correctedText.take(800)
                if (v != null) {
                    val record = MedicalRecord(patientId = patientId, type = "化验单(OCR)", content = summary)
                    val rid = db.medicalRecordDao().insert(record)
                    db.vitalSignsDao().insert(v.copy(recordId = rid))
                    withContext(Dispatchers.Main) {
                        loadVitalSummary(); loadRecords()
                        Toast.makeText(this@PatientDetailActivity, "识别成功！已保存", Toast.LENGTH_SHORT).show()
                        // 提供 AI 校准选项
                        AlertDialog.Builder(this@PatientDetailActivity, R.style.GlassDialog)
                            .setTitle("识别成功")
                            .setMessage("本地 OCR 已保存。\n是否使用 AI 校准提高准确率？")
                            .setPositiveButton("不需要") { _, _ -> }
                            .setNeutralButton("AI 校准") { _, _ ->
                                ocrBmp?.let { bmp ->
                                    doPatientQwenCalibrate(bmp, rawText, patientId)
                                } ?: Toast.makeText(this@PatientDetailActivity, "图片加载失败", Toast.LENGTH_SHORT).show()
                            }.show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val root = LinearLayout(this@PatientDetailActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 12, 40, 12) }
                        val et = EditText(this@PatientDetailActivity).apply { setText(rawText); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; minLines = 10; maxLines = 20; gravity = android.view.Gravity.TOP; setTypeface(Typeface.MONOSPACE); textSize = 11f; setHorizontallyScrolling(true) }
                        root.addView(TextView(this@PatientDetailActivity).apply { text = "识别文字如下，可修改后重新解析："; textSize = 12f; setTextColor(Color.GRAY); setPadding(0,0,0,4) })
                        root.addView(et)
                        AlertDialog.Builder(this@PatientDetailActivity, R.style.GlassDialog).setTitle("手动修正").setView(root)
                            .setPositiveButton("重新解析") { _, _ ->
                                val txt = et.text.toString()
                                val vt = com.mynote.android.util.VitalSignsParser.parse(patientId, 0, txt)
                                val (vv, pp) = parseLabLoose(txt)
                                val finalVs = vt ?: vv
                                val content = if (pp.isNotBlank()) pp + "\n\n── 原始文字 ──\n" + txt.take(800) else txt.take(800)
                                if (finalVs != null) {
                                    lifecycleScope.launch {
                                        val rid = db.medicalRecordDao().insert(MedicalRecord(patientId = patientId, type = "化验单(OCR)", content = content))
                                        db.vitalSignsDao().insert(finalVs.copy(recordId = rid))
                                        loadVitalSummary(); loadRecords()
                                    }
                                    Toast.makeText(this@PatientDetailActivity, "已保存", Toast.LENGTH_SHORT).show()
                                }
                                else Toast.makeText(this@PatientDetailActivity, "仍无法解析", Toast.LENGTH_SHORT).show()
                            }
                            .setNeutralButton("AI 识别校准") { _, _ ->
                                ocrBmp?.let { bmp ->
                                    Toast.makeText(this@PatientDetailActivity, "正在调用 Qwen-VL...", Toast.LENGTH_LONG).show()
                                    doPatientQwenCalibrate(bmp, rawText, patientId)
                                } ?: Toast.makeText(this@PatientDetailActivity, "图片加载失败", Toast.LENGTH_SHORT).show()
                            }.setNegativeButton("取消", null).create().apply {
                                show()
                                window?.setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                            }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@PatientDetailActivity, "识别失败: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun doOcrLabNoPhoto() { showManualLabInput() }

    private fun parseLabLoose(text: String): Pair<com.mynote.android.data.entity.VitalSigns?, String> {
        fun n(s: String) = Regex("""([\d.]+)""").find(s)?.groupValues?.get(1)?.toDoubleOrNull()
        val extras = sortedMapOf<String, String>()
        val abnorms = mutableSetOf<String>()
        val ref = mapOf(
            "WBC" to (4.0 to 10.0), "RBC" to (4.0 to 5.5), "Hb" to (120.0 to 160.0),
            "HCT" to (37.0 to 50.0), "MCV" to (80.0 to 100.0), "PLT" to (100.0 to 300.0),
            "NEUT%" to (40.0 to 75.0), "LYMPH%" to (20.0 to 40.0),
            "ALT" to (0.0 to 40.0), "AST" to (0.0 to 40.0), "Alb" to (35.0 to 55.0),
            "TBil" to (3.4 to 20.5), "DBil" to (0.0 to 6.8), "GGT" to (10.0 to 60.0),
            "ALP" to (45.0 to 125.0), "Cr" to (44.0 to 133.0), "BUN" to (2.9 to 8.2),
            "UA" to (150.0 to 420.0), "eGFR" to (60.0 to 999.0),
            "K⁺" to (3.5 to 5.3), "Na⁺" to (135.0 to 145.0), "Cl⁻" to (96.0 to 108.0),
            "Ca" to (2.1 to 2.6), "Mg" to (0.75 to 1.02), "P" to (0.81 to 1.45),
            "LDL" to (0.0 to 3.4), "HDL" to (1.0 to 1.9), "TC" to (3.0 to 5.2),
            "TG" to (0.0 to 1.7), "HbA1c" to (4.0 to 6.0), "血糖" to (3.9 to 6.1),
            "TSH" to (0.27 to 4.2), "FT3" to (3.1 to 6.8), "FT4" to (12.0 to 22.0),
            "CK" to (38.0 to 174.0), "CK-MB" to (0.0 to 24.0), "TnI" to (0.0 to 0.04),
            "BNP" to (0.0 to 100.0), "D-Dimer" to (0.0 to 0.5), "CRP" to (0.0 to 8.0),
            "INR" to (0.8 to 1.2), "PT" to (9.6 to 13.0), "APTT" to (24.0 to 36.0),
            "AMY" to (25.0 to 115.0), "LIP" to (5.6 to 51.3),
            "体温" to (36.0 to 37.3), "SpO₂" to (95.0 to 100.0),
        )
        fun s(line: String, key: String): String? {
            val nv = n(line) ?: return null
            val r = ref[key]; if (r != null && (nv < r.first || nv > r.second)) abnorms.add(key)
            return "$key: $nv"
        }
        var temp: Double? = null; var pulse: Int? = null; var resp: Int? = null
        var sys: Int? = null; var dia: Int? = null; var spo2: Int? = null
        var hba1c: Double? = null; var glu: Double? = null; var cr: Double? = null
        var egfr: Double? = null; var k: Double? = null; var na: Double? = null
        var hb: Double? = null; var alb: Double? = null; var alt: Double? = null; var ast: Double? = null; var ldl: Double? = null

        for (line in text.lines()) {
            val l = line.trim().lowercase(); val v = n(l) ?: continue
            when {
                // 血常规
                l.contains("wbc") || Regex("""(?:白细胞|白血球)""").containsMatchIn(l) -> { s(l, "WBC")?.let { extras["WBC"] = it } }
                l.contains("rbc") || Regex("""(?:红细胞|红血球)(?!.*压积|.*宽度)""").containsMatchIn(l) -> { s(l, "RBC")?.let { extras["RBC"] = it } }
                l.contains("hgb") || l.contains("hb ") || Regex("""(?:血红蛋白)(?!.*a1)""").containsMatchIn(l) -> { hb = v; extras["Hb"] = "Hb: $v g/L" }
                l.contains("hct") || l.contains("红细胞压积") -> { s(l, "HCT")?.let { extras["HCT"] = it } }
                l.contains("mcv") || l.contains("平均红细胞体积") -> { s(l, "MCV")?.let { extras["MCV"] = it } }
                l.contains("mch ") || l.contains("平均血红蛋白量") -> { s(l, "MCH")?.let { extras["MCH"] = it } }
                l.contains("mchc") || l.contains("平均血红蛋白浓度") -> { s(l, "MCHC")?.let { extras["MCHC"] = it } }
                l.contains("plt") || l.contains("血小板") -> { s(l, "PLT")?.let { extras["PLT"] = it } }
                l.contains("rdw") || l.contains("红细胞分布宽度") -> { s(l, "RDW")?.let { extras["RDW"] = it } }
                l.contains("mpv") || l.contains("平均血小板体积") -> { s(l, "MPV")?.let { extras["MPV"] = it } }
                l.contains("pct") && !l.contains("crp") -> { s(l, "PCT")?.let { extras["PCT"] = it } }
                l.contains("neut%") || l.contains("中性粒细胞%") -> { s(l, "NEUT%")?.let { extras["NEUT%"] = it } }
                l.contains("neut#") || l.contains("中性粒细胞#") -> { s(l, "NEUT#")?.let { extras["NEUT#"] = it } }
                l.contains("lymph%") || l.contains("淋巴细胞%") -> { s(l, "LYMPH%")?.let { extras["LYMPH%"] = it } }
                l.contains("lymph#") || l.contains("淋巴细胞#") -> { s(l, "LYMPH#")?.let { extras["LYMPH#"] = it } }
                l.contains("mono%") || l.contains("单核细胞%") -> { s(l, "MONO%")?.let { extras["MONO%"] = it } }
                l.contains("eo%") || l.contains("嗜酸性%") -> { s(l, "EO%")?.let { extras["EO%"] = it } }
                l.contains("baso%") || l.contains("嗜碱性%") -> { s(l, "BASO%")?.let { extras["BASO%"] = it } }
                // 肝功能
                l.contains("alt") || l.contains("谷丙") -> { alt = v; extras["ALT"] = "ALT: $v U/L" }
                l.contains("ast") || l.contains("谷草") -> { ast = v; extras["AST"] = "AST: $v U/L" }
                l.contains("alb") || l.contains("白蛋白") -> { alb = v; extras["Alb"] = "Alb: $v g/L" }
                l.contains("tbil") || l.contains("tbil") || l.contains("总胆红素") -> { s(l, "TBil")?.let { extras["TBil"] = it } }
                l.contains("dbil") || l.contains("直接胆红素") -> { s(l, "DBil")?.let { extras["DBil"] = it } }
                l.contains("ggt") || l.contains("谷氨酰") -> { s(l, "GGT")?.let { extras["GGT"] = it } }
                l.contains("alp") || l.contains("碱性磷酸酶") -> { s(l, "ALP")?.let { extras["ALP"] = it } }
                // 肾功能
                l.contains("肌酐") || l.contains("cr ") || l.contains("scr ") -> { cr = v; extras["Cr"] = "Cr: $v" }
                l.contains("尿素") || l.contains("bun") -> { s(l, "BUN")?.let { extras["BUN"] = it } }
                l.contains("尿酸") || l.contains("ua ") -> { s(l, "UA")?.let { extras["UA"] = it } }
                l.contains("egfr") || l.contains("gfr") -> { egfr = v; extras["eGFR"] = "eGFR: $v" }
                // 电解质
                l.contains("k+") || l.contains("k⁺") || l.contains("钾") -> { k = v; extras["K⁺"] = "K⁺: $v" }
                l.contains("na+") || l.contains("na⁺") || l.contains("钠") -> { na = v; extras["Na⁺"] = "Na⁺: $v" }
                l.contains("cl-") || l.contains("氯") -> { s(l, "Cl⁻")?.let { extras["Cl⁻"] = it } }
                l.contains("ca") || Regex("""(?:血钙)(?!#)""").containsMatchIn(l) -> { s(l, "Ca")?.let { extras["Ca"] = it } }
                // 血脂
                l.contains("ldl") || l.contains("低密度") -> { ldl = v; extras["LDL"] = "LDL: $v" }
                l.contains("hdl") || l.contains("高密度") -> { s(l, "HDL")?.let { extras["HDL"] = it } }
                l.contains("tc") || l.contains("总胆固醇") -> { s(l, "TC")?.let { extras["TC"] = it } }
                l.contains("tg") || l.contains("甘油三酯") -> { s(l, "TG")?.let { extras["TG"] = it } }
                // 心肌
                l.contains("ck") && !l.contains("ckd") -> { s(l, "CK")?.let { extras["CK"] = it } }
                l.contains("ck-mb") -> { s(l, "CK-MB")?.let { extras["CK-MB"] = it } }
                l.contains("tni") || l.contains("肌钙蛋白") -> { s(l, "TnI")?.let { extras["TnI"] = it } }
                l.contains("bnp") || l.contains("脑钠肽") -> { s(l, "BNP")?.let { extras["BNP"] = it } }
                l.contains("d-二聚体") || l.contains("d-dimer") -> { s(l, "D-Dimer")?.let { extras["D-Dimer"] = it } }
                // 炎症
                l.contains("crp") || l.contains("c反应蛋白") -> { s(l, "CRP")?.let { extras["CRP"] = it } }
                l.contains("esr") || l.contains("血沉") -> { s(l, "ESR")?.let { extras["ESR"] = it } }
                l.contains("降钙素原") || (l.contains("pct") && l.contains("降钙素")) -> { s(l, "PCT")?.let { extras["PCT"] = it } }
                // 凝血
                l.contains("pt ") && !l.contains("aptt") && !l.contains("tpt") -> { s(l, "PT")?.let { extras["PT"] = it } }
                l.contains("aptt") -> { s(l, "APTT")?.let { extras["APTT"] = it } }
                l.contains("inr") -> { s(l, "INR")?.let { extras["INR"] = it } }
                l.contains("fib") || l.contains("纤维蛋白原") -> { s(l, "FIB")?.let { extras["FIB"] = it } }
                l.contains("tt ") && !l.contains("aptt") -> { s(l, "TT")?.let { extras["TT"] = it } }
                // 尿常规
                l.contains("sg ") || l.contains("尿比重") -> { s(l, "SG")?.let { extras["SG"] = it } }
                l.contains("ph ") && !l.contains("phos") -> { s(l, "pH")?.let { extras["pH"] = it } }
                Regex("""(?:尿蛋白|pro\b)(?!.*calc|.*bnp)""").containsMatchIn(l) -> { s(l, "PRO")?.let { extras["PRO"] = it } }
                Regex("""(?:尿糖|尿葡萄糖)""").containsMatchIn(l) -> { s(l, "GLU")?.let { extras["GLU"] = it } }
                Regex("""(?:尿酮体|ket\b)""").containsMatchIn(l) -> { s(l, "KET")?.let { extras["KET"] = it } }
                Regex("""(?:尿胆红素|urobil)""").containsMatchIn(l) -> { s(l, "UBG")?.let { extras["UBG"] = it } }
                Regex("""(?:尿潜血|bld|ob\b)""").containsMatchIn(l) && l.contains("尿") -> { s(l, "BLD")?.let { extras["BLD"] = it } }
                Regex("""(?:尿白细胞|leu\b|尿白细)""").containsMatchIn(l) -> { s(l, "LEU")?.let { extras["LEU"] = it } }
                Regex("""(?:尿亚硝酸|nit\b)""").containsMatchIn(l) -> { s(l, "NIT")?.let { extras["NIT"] = it } }
                Regex("""(?:尿红细胞.*hpf|ur-?rbc)""").containsMatchIn(l) -> { s(l, "UR-RBC")?.let { extras["UR-RBC"] = it } }
                Regex("""(?:尿白细胞.*hpf|ur-?wbc)""").containsMatchIn(l) -> { s(l, "UR-WBC")?.let { extras["UR-WBC"] = it } }
                Regex("""(?:管型|cast)""").containsMatchIn(l) -> { s(l, "CAST")?.let { extras["CAST"] = it } }
                // 便常规
                Regex("""(?:便潜血|粪便隐血|fob)""").containsMatchIn(l) -> { s(l, "FOB")?.let { extras["FOB"] = it } }
                Regex("""(?:便红细胞|粪便红)""").containsMatchIn(l) -> { s(l, "便RBC")?.let { extras["便RBC"] = it } }
                Regex("""(?:便白细胞|粪便白)""").containsMatchIn(l) -> { s(l, "便WBC")?.let { extras["便WBC"] = it } }
                // 甲状腺
                l.contains("tsh") || l.contains("促甲状腺") -> { s(l, "TSH")?.let { extras["TSH"] = it } }
                l.contains("ft3") || l.contains("游离t3") -> { s(l, "FT3")?.let { extras["FT3"] = it } }
                l.contains("ft4") || l.contains("游离t4") -> { s(l, "FT4")?.let { extras["FT4"] = it } }
                l.contains("tt3") || l.contains("t3 ") -> { s(l, "T3")?.let { extras["T3"] = it } }
                l.contains("tt4") || l.contains("t4 ") -> { s(l, "T4")?.let { extras["T4"] = it } }
                l.contains("tpoab") || l.contains("过氧化物酶抗体") -> { s(l, "TPOAb")?.let { extras["TPOAb"] = it } }
                l.contains("tgab") || l.contains("球蛋白抗体") -> { s(l, "TGAb")?.let { extras["TGAb"] = it } }
                // 肿瘤标志物
                l.contains("afp") || l.contains("甲胎蛋白") -> { s(l, "AFP")?.let { extras["AFP"] = it } }
                l.contains("cea") || l.contains("癌胚抗原") -> { s(l, "CEA")?.let { extras["CEA"] = it } }
                l.contains("ca199") || l.contains("ca19-9") -> { s(l, "CA199")?.let { extras["CA199"] = it } }
                l.contains("ca125") || l.contains("ca-125") -> { s(l, "CA125")?.let { extras["CA125"] = it } }
                l.contains("ca153") || l.contains("ca15-3") -> { s(l, "CA153")?.let { extras["CA153"] = it } }
                l.contains("psa") && !l.contains("fpsa") -> { s(l, "PSA")?.let { extras["PSA"] = it } }
                l.contains("fpsa") || l.contains("游离psa") -> { s(l, "fPSA")?.let { extras["fPSA"] = it } }
                // 激素
                l.contains("fsh") || l.contains("卵泡刺激素") -> { s(l, "FSH")?.let { extras["FSH"] = it } }
                l.contains("lh ") || l.contains("黄体生成素") -> { s(l, "LH")?.let { extras["LH"] = it } }
                l.contains("e2 ") || l.contains("雌二醇") -> { s(l, "E2")?.let { extras["E2"] = it } }
                l.contains("prl") || l.contains("泌乳素") -> { s(l, "PRL")?.let { extras["PRL"] = it } }
                Regex("""(?:孕酮|prog\b)""").containsMatchIn(l) -> { s(l, "PROG")?.let { extras["PROG"] = it } }
                l.contains("test") || l.contains("睾酮") -> { s(l, "TEST")?.let { extras["TEST"] = it } }
                l.contains("cortisol") || l.contains("皮质醇") -> { s(l, "Cortisol")?.let { extras["Cortisol"] = it } }
                l.contains("amh") || l.contains("抗苗勒") -> { s(l, "AMH")?.let { extras["AMH"] = it } }
                l.contains("hcg") || l.contains("绒毛膜") -> { s(l, "hCG")?.let { extras["hCG"] = it } }
                // 血气
                Regex("""(?:ph[\s:＝].*\d)""").containsMatchIn(l) && !l.contains("phos") -> { s(l, "pH")?.let { extras["pH"] = it } }
                l.contains("pco2") || l.contains("二氧化碳分压") -> { s(l, "pCO₂")?.let { extras["pCO₂"] = it } }
                l.contains("po2") || l.contains("氧分压") -> { s(l, "pO₂")?.let { extras["pO₂"] = it } }
                l.contains("hco3") || Regex("""(?:碳酸氢根|实际碳酸)""").containsMatchIn(l) -> { s(l, "HCO₃")?.let { extras["HCO₃"] = it } }
                l.contains("be ") || l.contains("碱剩余") -> { s(l, "BE")?.let { extras["BE"] = it } }
                l.contains("lac") || l.contains("乳酸") -> { s(l, "Lac")?.let { extras["Lac"] = it } }
                // 贫血
                l.contains("fe ") || l.contains("血清铁") -> { s(l, "Fe")?.let { extras["Fe"] = it } }
                l.contains("ferritin") || l.contains("铁蛋白") -> { s(l, "Ferritin")?.let { extras["Ferritin"] = it } }
                l.contains("tibc") || l.contains("总铁结合力") -> { s(l, "TIBC")?.let { extras["TIBC"] = it } }
                Regex("""(?:b12|维生素b12)""").containsMatchIn(l) -> { s(l, "B12")?.let { extras["B12"] = it } }
                l.contains("folate") || l.contains("叶酸") -> { s(l, "Folate")?.let { extras["Folate"] = it } }
                // 免疫
                l.contains("ana") || l.contains("抗核抗体") -> { s(l, "ANA")?.let { extras["ANA"] = it } }
                l.contains("dsdna") || l.contains("ds-dna") || l.contains("双链dna") -> { s(l, "dsDNA")?.let { extras["dsDNA"] = it } }
                l.contains("rf ") || l.contains("类风湿因子") -> { s(l, "RF")?.let { extras["RF"] = it } }
                l.contains("c3 ") || l.contains("补体c3") -> { s(l, "C3")?.let { extras["C3"] = it } }
                l.contains("c4 ") || l.contains("补体c4") -> { s(l, "C4")?.let { extras["C4"] = it } }
                l.contains("ige") || l.contains("免疫球蛋白e") -> { s(l, "IgE")?.let { extras["IgE"] = it } }
                // 病毒血清
                l.contains("hbsag") || l.contains("乙肝表面抗原") -> { s(l, "HBsAg")?.let { extras["HBsAg"] = it } }
                l.contains("hbsab") || l.contains("乙肝表面抗体") -> { s(l, "HBsAb")?.let { extras["HBsAb"] = it } }
                l.contains("hbeag") || l.contains("乙肝e抗原") -> { s(l, "HBeAg")?.let { extras["HBeAg"] = it } }
                l.contains("hbeab") || l.contains("乙肝e抗体") -> { s(l, "HBeAb")?.let { extras["HBeAb"] = it } }
                l.contains("hbcab") || l.contains("乙肝核心抗体") -> { s(l, "HBcAb")?.let { extras["HBcAb"] = it } }
                l.contains("hcv") || l.contains("丙肝") -> { s(l, "HCV")?.let { extras["HCV"] = it } }
                l.contains("hiv") || l.contains("艾滋") -> { s(l, "HIV")?.let { extras["HIV"] = it } }
                // 药物浓度
                Regex("""(?:地高辛|digoxin|dig\b)""").containsMatchIn(l) -> { s(l, "Digoxin")?.let { extras["Digoxin"] = it } }
                Regex("""(?:丙戊酸|vpa|valpro)""").containsMatchIn(l) -> { s(l, "VPA")?.let { extras["VPA"] = it } }
                l.contains("carbamaz") || l.contains("卡马西平") -> { s(l, "CBZ")?.let { extras["CBZ"] = it } }
                l.contains("phenytoin") || l.contains("苯妥英") -> { s(l, "PHT")?.let { extras["PHT"] = it } }
                // 淀粉酶
                l.contains("amy") || l.contains("淀粉酶") -> { s(l, "AMY")?.let { extras["AMY"] = it } }
                l.contains("lipase") || l.contains("脂肪酶") -> { s(l, "LIP")?.let { extras["LIP"] = it } }
                // 免疫球蛋白
                l.contains("igg") && !l.contains("ige") && !l.contains("hbeag") -> { s(l, "IgG")?.let { extras["IgG"] = it } }
                l.contains("iga") -> { s(l, "IgA")?.let { extras["IgA"] = it } }
                l.contains("igm") -> { s(l, "IgM")?.let { extras["IgM"] = it } }
                // 骨代谢
                l.contains("pth") || l.contains("甲状旁腺激素") -> { s(l, "PTH")?.let { extras["PTH"] = it } }
                Regex("""(?:25.*vit.?d|25羟维生素|25-oh-d)""").containsMatchIn(l) -> { s(l, "25-OH-VitD")?.let { extras["25-OH-VitD"] = it } }
                l.contains("calciton") && !l.contains("procalc") -> { s(l, "CT")?.let { extras["CT"] = it } }
                // 微量
                l.contains("mg ") || l.contains("镁") -> { s(l, "Mg")?.let { extras["Mg"] = it } }
                Regex("""(?:血磷|磷\b|phos[^p])""").containsMatchIn(l) -> { s(l, "P")?.let { extras["P"] = it } }
                l.contains("渗透压") || l.contains("osm") -> { s(l, "Osm")?.let { extras["Osm"] = it } }
                // 心脏补充
                l.contains("hs-ctni") || l.contains("hstni") || l.contains("高敏肌钙") -> { s(l, "hs-cTnI")?.let { extras["hs-cTnI"] = it } }
                l.contains("nt-probnp") || l.contains("nt-pro-bnp") -> { s(l, "NT-proBNP")?.let { extras["NT-proBNP"] = it } }
                l.contains("myo") && !l.contains("myocard") -> { s(l, "MYO")?.let { extras["MYO"] = it } }
                // 自身抗体
                l.contains("ccp") || l.contains("抗环瓜") -> { s(l, "anti-CCP")?.let { extras["anti-CCP"] = it } }
                l.contains("anca") || l.contains("抗中性粒") -> { s(l, "ANCA")?.let { extras["ANCA"] = it } }
                l.contains("ssa") || l.contains("抗ssa") || l.contains("抗ro") -> { s(l, "anti-SSA")?.let { extras["anti-SSA"] = it } }
                l.contains("ssb") || l.contains("抗ssb") || l.contains("抗la") -> { s(l, "anti-SSB")?.let { extras["anti-SSB"] = it } }
                l.contains("抗sm") || l.contains("anti-sm") -> { s(l, "anti-Sm")?.let { extras["anti-Sm"] = it } }
                // T细胞亚群
                l.contains("cd4") || l.contains("cd4+") -> { s(l, "CD4")?.let { extras["CD4"] = it } }
                l.contains("cd8") || l.contains("cd8+") -> { s(l, "CD8")?.let { extras["CD8"] = it } }
                l.contains("cd4/cd8") -> { s(l, "CD4/CD8")?.let { extras["CD4/CD8"] = it } }
                // 维生素/微量
                Regex("""(?:叶酸|folate)""").containsMatchIn(l) -> { s(l, "Folate")?.let { extras["Folate"] = it } }
                l.contains("铜蓝蛋白") || l.contains("ceruloplasmin") -> { s(l, "CP")?.let { extras["CP"] = it } }
                l.contains("锌") || l.contains("zn ") -> { s(l, "Zn")?.let { extras["Zn"] = it } }
                // 特殊体液
                Regex("""(?:脑脊液|cerebrospinal|csf)""").containsMatchIn(l) -> {
                    Regex("""(?:csf|脑脊液)(?:.*?)(?:蛋白|wbc|rbc|糖|氯)""").containsMatchIn(l)?.let { s(l, "CSF")?.let { extras["CSF"] = it } }
                }
                // 胸腹水
                Regex("""(?:胸水|腹水|胸腹水|pleural|ascites)""").containsMatchIn(l) -> { s(l, "体液")?.let { extras["体液"] = it } }
                // Beta-hCG (区分于总hCG)
                l.contains("β-hcg") || l.contains("beta-hcg") -> { s(l, "β-hCG")?.let { extras["β-hCG"] = it } }
                // 5'-核苷酸酶
                l.contains("5'-nt") || l.contains("5-核苷") -> { s(l, "5'-NT")?.let { extras["5'-NT"] = it } }
                // 腺苷脱氨酶
                l.contains("ada") || l.contains("腺苷脱氨") -> { s(l, "ADA")?.let { extras["ADA"] = it } }
                // 肝纤四项
                l.contains("ha ") || l.contains("透明质酸") -> { s(l, "HA")?.let { extras["HA"] = it } }
                l.contains("ln ") || l.contains("层粘连蛋白") -> { s(l, "LN")?.let { extras["LN"] = it } }
                Regex("""(?:pciii|pciii|iii型前胶原)""").containsMatchIn(l) -> { s(l, "PCIII")?.let { extras["PCIII"] = it } }
                Regex("""(?:civ|c-iv|iv型胶原)""").containsMatchIn(l) -> { s(l, "C-IV")?.let { extras["C-IV"] = it } }
                // 蛋白电泳
                l.contains("m蛋白") || l.contains("m-protein") -> { s(l, "M-Protein")?.let { extras["M-Protein"] = it } }
                // 糖耐量
                l.contains("ogtt") || l.contains("糖耐量") -> { s(l, "OGTT")?.let { extras["OGTT"] = it } }
                Regex("""(?:餐后.*血糖|2h.*血糖|2hbg)""").containsMatchIn(l) -> { s(l, "2hPG")?.let { extras["2hPG"] = it } }
                // 21/18/13三体筛查
                Regex("""(?:21三体|t21|唐氏)""").containsMatchIn(l) -> { s(l, "T21")?.let { extras["T21"] = it } }
                Regex("""(?:18三体|t18)""").containsMatchIn(l) -> { s(l, "T18")?.let { extras["T18"] = it } }
                Regex("""(?:13三体|t13)""").containsMatchIn(l) -> { s(l, "T13")?.let { extras["T13"] = it } }
                // 脑利钠肽补充
                l.contains("nt-probnp") || l.contains("氨基末端脑钠") -> { s(l, "NT-proBNP")?.let { extras["NT-proBNP"] = it } }
                // 原有体征
                l.contains("体温") || l.startsWith("t ") || l.contains("temp") -> { temp = v; extras["体温"] = "${v}℃" }
                l.contains("脉搏") || (l.startsWith("p") && !l.startsWith("pa") && !l.startsWith("pt")) -> { pulse = v.toInt(); extras["脉搏"] = "$pulse 次/分" }
                l.contains("呼吸") || (l.startsWith("r") && !l.contains("r-") && !l.startsWith("rbc")) -> { resp = v.toInt(); extras["呼吸"] = "$resp 次/分" }
                Regex("""(?:高压|收缩压|sbp)""").containsMatchIn(l) -> { sys = v.toInt(); extras["收缩压"] = "$sys" }
                Regex("""(?:低压|舒张压|dbp)""").containsMatchIn(l) -> { dia = v.toInt(); extras["舒张压"] = "$dia" }
                l.contains("bp") || l.contains("血压") -> { val m = Regex("""(\d+)\s*/?\s*(\d+)""").find(l); if (m != null) { sys=m.groupValues[1].toIntOrNull(); dia=m.groupValues[2].toIntOrNull(); extras["血压"] = "$sys/$dia" } }
                l.contains("spo") || l.contains("血氧") -> { spo2 = v.toInt(); extras["SpO₂"] = "$spo2%" }
                l.contains("hba1c") || l.contains("糖化") -> { hba1c = v; extras["HbA1c"] = "$v%" }
                Regex("""(?:空腹)?血糖|glu|glucose""").containsMatchIn(l) -> { glu = v; extras["血糖"] = "$v" }
            }
            // ═══ 兜底：未匹配的行尝试万能正则 ═══
            val unknown = Regex("""([\u4e00-\u9fa5a-zA-Z/\-+]+)\s*[:：=]?\s*([\d.]+)\s*(?:x10\^?\d+/\w+|[a-zA-Z/·]+)?""").find(l)
            if (unknown != null) {
                val key = unknown.groupValues[1].trim().take(20)
                val blacklist = setOf("of","in","is","at","to","by","or","mm","ml","mg","g ","l ","dl","ul","kg","cm","nm","cfu")
                if (!extras.containsKey(key) && key.length >= 2 && key.lowercase() !in blacklist) {
                    extras[key] = "$key: ${unknown.groupValues[2]}"
                }
            }
        }

        val vs = if (temp != null || pulse != null || sys != null || hba1c != null || cr != null || k != null || hb != null)
            com.mynote.android.data.entity.VitalSigns(patientId=patientId, recordId=0, temperature=temp, pulse=pulse, respiration=resp, bpSystolic=sys, bpDiastolic=dia, spo2=spo2, hba1c=hba1c, fastingGlu=glu, creatinine=cr, egfr=egfr, potassium=k, sodium=na, hemoglobin=hb, albumin=alb, alt=alt, ast=ast, ldl=ldl)
        else null

        // 统一检查异常值：遍历 extras 每一项，提取数值比对参考范围
        for ((key, entry) in extras) {
            if (key in abnorms) continue // s() 已标记过的
            val nv = n(entry) ?: continue
            // 尝试匹配 ref 中的 key（直接或近似）
            if (ref.containsKey(key)) {
                val r = ref[key]!!
                if (nv < r.first || nv > r.second) abnorms.add(key)
            }
        }
        // 特殊值：血压
        if (sys != null && (sys!! < 90 || sys!! > 140)) abnorms.add("血压")
        if (dia != null && (dia!! < 60 || dia!! > 90)) abnorms.add("血压")
        if (spo2 != null && spo2!! < 95) abnorms.add("SpO₂")

        // ⚠️ 标记异常 + 两列排版
        val items = extras.values.map { v ->
            val k = v.substringBefore(":")
            if (k in abnorms) "⚠️$v" else v
        }
        val maxLen = items.maxOfOrNull { it.length } ?: 20
        val colWidth = (maxLen + 6).coerceIn(22, 42)
        val summary = buildString {
            if (abnorms.isNotEmpty()) append("⚠️ 异常项: ${abnorms.joinToString(" ")}\n\n")
            for (i in items.indices step 2) {
                val left = items[i].padEnd(colWidth)
                val right = items.getOrElse(i + 1) { "" }
                append(left).append(right)
                if (i + 2 < items.size) append("\n")
            }
        }
        return Pair(vs, summary)
    }

    private fun showManualLabInput() {
        val fields = listOf("体温 ℃","脉搏","呼吸","收缩压","舒张压","SpO₂","HbA1c","肌酐","eGFR","K⁺","Na⁺","Hb")
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 12, 40, 0) }
        val ets = fields.map { EditText(this).apply { hint = it; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setSingleLine(true) }.also { root.addView(it) } }
        AlertDialog.Builder(this, R.style.GlassDialog).setTitle("逐项输入").setView(root)
            .setPositiveButton("保存") { _, _ ->
                val vs = com.mynote.android.data.entity.VitalSigns(patientId=patientId, recordId=0,
                    temperature=ets[0].text.toString().toDoubleOrNull(), pulse=ets[1].text.toString().toIntOrNull(),
                    respiration=ets[2].text.toString().toIntOrNull(), bpSystolic=ets[3].text.toString().toIntOrNull(),
                    bpDiastolic=ets[4].text.toString().toIntOrNull(), spo2=ets[5].text.toString().toIntOrNull(),
                    hba1c=ets[6].text.toString().toDoubleOrNull(), creatinine=ets[7].text.toString().toDoubleOrNull(),
                    egfr=ets[8].text.toString().toDoubleOrNull(), potassium=ets[9].text.toString().toDoubleOrNull(),
                    sodium=ets[10].text.toString().toDoubleOrNull(), hemoglobin=ets[11].text.toString().toDoubleOrNull())
                lifecycleScope.launch { db.vitalSignsDao().insert(vs); loadVitalSummary() }
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("取消", null).show()
    }

    private fun printRecordsPDF() {
        lifecycleScope.launch(Dispatchers.IO) {
            val records = db.medicalRecordDao().getByPatientSync(patientId)
            if (records.isEmpty()) { withContext(Dispatchers.Main) { Toast.makeText(this@PatientDetailActivity, "无病历", Toast.LENGTH_SHORT).show() }; return@launch }
            val file = File(cacheDir, "${patient.name}_全部病历.pdf")
            com.mynote.android.util.PdfExporter.export(patient, records, file)
            withContext(Dispatchers.Main) {
                val uri = androidx.core.content.FileProvider.getUriForFile(this@PatientDetailActivity, "$packageName.fileprovider", file)
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "打印/分享 PDF"))
            }
        }
    }

    private fun showMedicationCalendar() {
        lifecycleScope.launch {
            val records = db.medicalRecordDao().getByPatientSync(patientId)
            val drugs = mutableSetOf<String>()
            // 负向后顾：前面不能是汉字；只匹配常见药物后缀，激素/维生素太宽泛单独排除
            val rx = Regex("""(?<![\u4e00-\u9fa5])([\u4e00-\u9fa5]{1,3}(?:素|平|普利|沙坦|洛尔|地平|他汀|唑仑|拉唑|替丁|西林|环素|沙星|康唑|米松|龙|芬|昔布|考昔|帕尼|瑞克|佐辛|曲坦|吉兰|鲁胺|司琼|格列|达帕|列净|格列净|列汀|波糖)\s*\d*[mgμg片粒袋支丸装瓶]*)""")
            val tooBroad = setOf("激素", "维生素")
            val badStart = setOf("但", "则", "及", "型", "高", "低", "长", "短", "速", "缓")
            for (r in records) {
                rx.findAll(r.content).forEach { m ->
                    val d = m.value.trim()
                    if (d.length in 3..20 &&
                        tooBroad.none { d.endsWith(it) && d != "胰岛素" } &&
                        d.first().toString() !in badStart) drugs.add(d)
                }
            }
            val sb = StringBuilder()
            if (drugs.isEmpty()) sb.append("未检测到用药信息")
            else {
                sb.append("📋 ${patient.name} 用药清单\n\n")
                drugs.sorted().forEachIndexed { i, d -> sb.append("${i+1}. $d\n") }
                sb.append("\n⚠️ 从病历自动提取，仅供参考")
            }
            AlertDialog.Builder(this@PatientDetailActivity, R.style.GlassDialog)
                .setTitle("用药日历").setMessage(sb.toString()).setPositiveButton("关闭", null).show()
        }
    }

    private fun showFollowupDialog() {
        val prefs = getSharedPreferences("followup_prefs", MODE_PRIVATE)
        val current = prefs.getString("followup_$patientId", "") ?: ""
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 16, 40, 0) }
        val et = EditText(this).apply { hint = "yyyy-MM-dd"; setSingleLine(true); if (current.isNotEmpty()) setText(current) }
        root.addView(et)
        root.addView(TextView(this).apply { text = if (current.isNotEmpty()) "当前: $current" else "未设置"; textSize = 12f; setTextColor(Color.GRAY); setPadding(0, 8, 0, 0) })
        AlertDialog.Builder(this, R.style.GlassDialog).setTitle("随访提醒").setView(root)
            .setPositiveButton("保存") { _, _ ->
                val d = et.text.toString().trim()
                prefs.edit().putString("followup_$patientId", d).apply()
                if (d.isNotEmpty()) Toast.makeText(this@PatientDetailActivity, "已设置: $d", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("取消", null).show()
    }

    private fun exportRecords(format: String = "txt") {
        lifecycleScope.launch(Dispatchers.IO) {
            val records = db.medicalRecordDao().getByPatientSync(patientId)
            if (records.isEmpty()) {
                withContext(Dispatchers.Main) { Toast.makeText(this@PatientDetailActivity, "无病历", Toast.LENGTH_SHORT).show() }
                return@launch
            }
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val ts = sdf.format(Date())
            val suffix = format
            val file = File(cacheDir, "${patient.name}_病历_$ts.$suffix")

            when (format) {
                "md" -> exportMd(file, records)
                "docx" -> exportDocx(file, records)
                "pdf" -> com.mynote.android.util.PdfExporter.export(patient, records, file)
                else -> exportTxt(file, records)
            }

            withContext(Dispatchers.Main) {
                val uri = androidx.core.content.FileProvider.getUriForFile(this@PatientDetailActivity, "$packageName.fileprovider", file)
                val mime = when (format) { "md" -> "text/markdown"; "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; "pdf" -> "application/pdf"; else -> "text/plain" }
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "导出病历"))
            }
        }
    }

    private fun exportTxt(file: File, records: List<MedicalRecord>) {
        val sb = StringBuilder()
        sb.append("${patient.name} · ${patient.department} · ${patient.admissionDate}\n")
        sb.append("诊断: ${patient.diagnosis}\n${"=".repeat(40)}\n\n")
        for (r in records) {
            sb.append("【${r.type}】\n${r.content}\n\n${"=".repeat(40)}\n\n")
        }
        file.writeText(sb.toString())
    }

    private fun exportMd(file: File, records: List<MedicalRecord>) {
        val sb = StringBuilder()
        sb.append("# ${patient.name} 病历\n\n")
        sb.append("| 项目 | 内容 |\n|------|------|\n")
        sb.append("| 科室 | ${patient.department} |\n")
        sb.append("| 年龄 | ${patient.age} |\n")
        sb.append("| 性别 | ${patient.gender} |\n")
        sb.append("| 入院 | ${patient.admissionDate} |\n")
        sb.append("| 诊断 | ${patient.diagnosis} |\n\n---\n\n")
        for (r in records) {
            sb.append("## ${r.type}\n\n${r.content}\n\n---\n\n")
        }
        file.writeText(sb.toString())
    }

    private fun exportDocx(file: File, records: List<MedicalRecord>) {
        com.mynote.android.util.DocxWriter.writeMedicalRecords(file, patient, records)
    }

    /** 标红：【待补充】+ 入院诊断 + 初步诊断；加粗：指定段落标题 */
    private fun highlightPending(text: String): SpannableString {
        val ss = SpannableString(text)
        val red = ForegroundColorSpan(android.graphics.Color.RED)
        val bold = StyleSpan(Typeface.BOLD)

        // 标红：【待补充】及其变体（支持【待补充：说明】、[待补充] 等）
        for (pat in listOf("【待补充[^】]*】", "（待补充[^）]*）", "\\(待补充[^)]*\\)", "\\[待补充[^\\]]*\\]")) {
            Regex(pat).findAll(text).forEach { m ->
                ss.setSpan(red, m.range.first, m.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        // 标红：入院诊断、初步诊断
        for (h in listOf("入院诊断", "初步诊断")) {
            text.indicesOf(h).forEach { idx ->
                val before = if (idx > 0) text[idx - 1] else '\n'
                if (isBoundaryPrefix(before)) {
                    ss.setSpan(red, idx, idx + h.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        // 加粗：仅以下 9 个标题
        for (h in listOf("主诉", "现病史", "既往史", "个人史", "婚育史", "家族史", "体格检查", "辅助检查", "诊疗计划")) {
            text.indicesOf(h).forEach { idx ->
                val before = if (idx > 0) text[idx - 1] else '\n'
                if (isBoundaryPrefix(before)) {
                    val after = text.getOrNull(idx + h.length)
                    val len = if (after == '：' || after == ':') h.length + 1 else h.length
                    ss.setSpan(StyleSpan(Typeface.BOLD), idx, idx + len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        return ss
    }

    /** 返回字符串中所有子串匹配位置 */
    private fun String.indicesOf(sub: String): List<Int> {
        val list = mutableListOf<Int>()
        var i = indexOf(sub)
        while (i >= 0) { list.add(i); i = indexOf(sub, i + 1) }
        return list
    }

    /** 判断标题前字符是否为结构边界（非中英文字母数字，即标点/括号/空白等） */
    private fun isBoundaryPrefix(c: Char): Boolean =
        c.isWhitespace() || c == '\n' || c == '\r' || !c.isLetterOrDigit()

    // ===== 多轮对话继续改写 =====
    private var lastGeneratedText: String = ""

    private fun showContinueRewrite(dialog: AlertDialog, currentText: String,
                                     origParams: DeepSeekClient.GenParams,
                                     afterStream: ((AlertDialog, String) -> Unit)?,
                                     onDone: (String) -> Unit) {
        lastGeneratedText = currentText
        val et = EditText(this).apply {
            hint = "如：把诊断依据再详细一些、补充2024年指南推荐..."
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 16, 40, 0) }
        root.addView(et)

        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("继续改写")
            .setView(root)
            .setPositiveButton("发送") { _, _ ->
                val instruction = et.text.toString().trim()
                if (instruction.isEmpty()) return@setPositiveButton
                val prompt = "你是三甲医院主任医师。以下是病历原文：\n\n$lastGeneratedText\n\n用户要求：$instruction\n\n请在原文基础上修改，保留已有信息，只按用户要求调整。直接输出完整病历。"

                streamTv?.text = "⏳ 正在改写...\n"
                streamDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.visibility = View.GONE
                streamDialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.visibility = View.GONE

                DeepSeekClient.generateStreamWithPrompt(this@PatientDetailActivity, prompt, object : DeepSeekClient.StreamCallback {
                    override fun onToken(t: String) { streamTv?.post { streamTv?.append(t) } }
                    override fun onDone(newText: String) {
                        lastGeneratedText = newText
                        streamTv?.post {
                            streamTv?.text = newText + "\n\n✅ 改写完成"
                            streamDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                                visibility = View.VISIBLE; text = "保存"
                                setOnClickListener { streamDialog?.dismiss(); streamDialog = null; onDone(newText) }
                            }
                            streamDialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.visibility = View.VISIBLE
                            streamDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                                text = "关闭"
                                setOnClickListener { streamDialog?.dismiss(); streamDialog = null }
                            }
                        }
                    }
                    override fun onError(e: Exception) { streamTv?.post { streamTv?.append("\n❌ ${e.message}") } }
                })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ===== 语音输入 =====
    private fun launchVoiceInput(et: EditText) {
        voiceTargetEt = et
        try {
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "请说出需填写的内容")
            }
            startActivityForResult(intent, REQ_VOICE)
        } catch (e: Exception) {
            Toast.makeText(this, "设备不支持语音输入", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VOICE && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spoken = results?.firstOrNull() ?: return
            voiceTargetEt?.apply {
                val current = text.toString()
                setText(if (current.isNotEmpty()) "$current，$spoken" else spoken)
                setSelection(text.length)
            }
        }
    }
}

class RecordAdapter : RecyclerView.Adapter<RecordAdapter.VH>() {
    var onClick: ((MedicalRecord) -> Unit)? = null
    var onDelete: ((MedicalRecord) -> Unit)? = null
    private var list = listOf<MedicalRecord>()

    fun submitList(newList: List<MedicalRecord>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(list[position])

    override fun getItemCount() = list.size

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvType: TextView = v.findViewById(R.id.tv_r_type)
        private val tvPreview: TextView = v.findViewById(R.id.tv_r_preview)
        private val tvTime: TextView = v.findViewById(R.id.tv_r_time)
        private val tvTag: TextView = v.findViewById(R.id.tv_r_tag)
        private val btnDel: ImageButton = v.findViewById(R.id.btn_r_delete)

        fun bind(r: MedicalRecord) {
            tvType.text = r.type
            // 预览中标红【待补充】
            val ss = SpannableString(r.content.take(80).replace("\n", " "))
            val red = ForegroundColorSpan(android.graphics.Color.RED)
            Regex("【待补充[^】]*】").findAll(r.content.take(80)).forEach { m ->
                val end = (m.range.last + 1).coerceAtMost(ss.length)
                ss.setSpan(red, m.range.first, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            tvPreview.text = ss
            tvTime.text = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(r.createdAt))
            val s = com.mynote.android.util.RecordScorer.score(r.type, r.content)
            tvTag.text = "${com.mynote.android.util.RecordScorer.starLabel(s.total)} ${if (r.generatedBy == "AI") "🤖" else "✏️"}"
            itemView.setOnClickListener { onClick?.invoke(r) }
            btnDel.setOnClickListener { onDelete?.invoke(r) }
        }
    }
}