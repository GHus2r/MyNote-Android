package com.mynote.android.ui.patient

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mynote.android.R
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.Patient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PatientListActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var spinnerDept: Spinner
    private lateinit var tvEmpty: TextView
    private val db by lazy { AppDatabase.get(this) }
    private val adapter = PatientAdapter()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_list)

        rv = findViewById(R.id.rv_patients)
        etSearch = findViewById(R.id.et_search)
        spinnerDept = findViewById(R.id.spinner_dept)
        tvEmpty = findViewById(R.id.tv_empty_patient)
        val fab: Button = findViewById(R.id.fab_add)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        adapter.onItemClick = { patient ->
            startActivity(Intent(this, PatientDetailActivity::class.java).apply {
                putExtra("patient_id", patient.id)
            })
        }
        adapter.onLongClick = { patient -> showDeleteDialog(patient) }

        fab.setOnClickListener { showEditDialog(null) }

        // 科室筛选
        lifecycleScope.launch {
            val depts = db.patientDao().getAllDepartments()
            val deptList = mutableListOf("全部科室")
            deptList.addAll(depts.filter { it.isNotBlank() })
            val deptAdapter = ArrayAdapter(this@PatientListActivity, android.R.layout.simple_spinner_item, deptList)
            deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDept.adapter = deptAdapter
            spinnerDept.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    loadPatients(etSearch.text.toString())
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }

        // 搜索防抖 300ms
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                adapter.searchQuery = query
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    loadPatients(query)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadPatients("")
    }

    private fun loadPatients(query: String) {
        lifecycleScope.launch {
            val deptFilter = spinnerDept?.selectedItem?.toString().let {
                if (it == null || it == "全部科室") "" else it
            }
            val flow = if (query.isEmpty() && deptFilter.isEmpty()) {
                db.patientDao().getAll()
            } else {
                db.patientDao().searchFiltered(query, deptFilter)
            }
            flow.collectLatest { list ->
                adapter.submitList(list)
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                rv.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun showEditDialog(patient: Patient?) {
        val isEdit = patient != null
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }
        val fields = listOf("姓名" to (patient?.name ?: ""),
            "年龄" to (patient?.age?.toString() ?: ""),
            "性别" to (patient?.gender ?: ""),
            "床号" to (patient?.bedNumber ?: ""),
            "科室" to (patient?.department ?: ""),
            "入院日期" to (patient?.admissionDate ?: ""),
            "诊断" to (patient?.diagnosis ?: ""),
            "主诉" to (patient?.chiefComplaint ?: ""))
        val edits = fields.map { (label, value) ->
            EditText(this).apply { hint = label; setText(value); inputType = InputType.TYPE_CLASS_TEXT }
        }
        edits.forEach { root.addView(it) }

        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle(if (isEdit) "编辑患者" else "新增患者")
            .setView(root)
            .setPositiveButton("保存") { _, _ ->
                val p = Patient(
                    id = patient?.id ?: 0,
                    name = edits[0].text.toString().trim(),
                    age = edits[1].text.toString().trim().toIntOrNull() ?: 0,
                    gender = edits[2].text.toString().trim(),
                    bedNumber = edits[3].text.toString().trim(),
                    department = edits[4].text.toString().trim(),
                    admissionDate = edits[5].text.toString().trim(),
                    diagnosis = edits[6].text.toString().trim(),
                    chiefComplaint = edits[7].text.toString().trim()
                )
                lifecycleScope.launch {
                    if (isEdit) db.patientDao().update(p) else db.patientDao().insert(p)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteDialog(patient: Patient) {
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("删除患者")
            .setMessage("确定删除「${patient.name}」及其所有病历吗？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch { db.patientDao().delete(patient) }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

class PatientAdapter : RecyclerView.Adapter<PatientAdapter.VH>() {
    var onItemClick: ((Patient) -> Unit)? = null
    var onLongClick: ((Patient) -> Unit)? = null
    var searchQuery = ""

    private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(old: Patient, new: Patient) = old.id == new.id
        override fun areContentsTheSame(old: Patient, new: Patient) =
            old.name == new.name && old.age == new.age && old.gender == new.gender &&
            old.department == new.department && old.diagnosis == new.diagnosis &&
            old.bedNumber == new.bedNumber && old.admissionDate == new.admissionDate
    })

    fun submitList(newList: List<Patient>) {
        differ.submitList(newList.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_patient, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(differ.currentList[position])

    override fun getItemCount() = differ.currentList.size

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvName: TextView = v.findViewById(R.id.tv_p_name)
        private val tvMeta: TextView = v.findViewById(R.id.tv_p_meta)
        private val tvDiag: TextView = v.findViewById(R.id.tv_p_diag)

        fun bind(p: Patient) {
            val q = searchQuery
            tvName.text = if (q.isNotBlank()) highlight(p.name.ifEmpty { "未命名患者" }, q) else p.name.ifEmpty { "未命名患者" }
            val meta = "${p.age}岁 ${p.gender}  ${p.bedNumber}  ${p.department}"
            tvMeta.text = if (q.isNotBlank()) highlight(meta, q) else meta
            tvDiag.text = if (q.isNotBlank()) highlight(p.diagnosis, q) else p.diagnosis
            itemView.setOnClickListener { onItemClick?.invoke(p) }
            itemView.setOnLongClickListener { onLongClick?.invoke(p); true }
        }

        private fun highlight(text: String, query: String): SpannableString {
            val sp = SpannableString(text)
            val lower = text.lowercase()
            val q = query.lowercase()
            var start = 0
            while (true) {
                val idx = lower.indexOf(q, start)
                if (idx < 0) break
                sp.setSpan(BackgroundColorSpan(android.graphics.Color.parseColor("#FFEB3B")), idx, idx + query.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                start = idx + query.length
            }
            return sp
        }
    }
}
