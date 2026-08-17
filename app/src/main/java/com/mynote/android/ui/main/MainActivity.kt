package com.mynote.android.ui.main

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.ScrollView
import android.graphics.Color
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mynote.android.R
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.ContentItem
import com.mynote.android.data.entity.Note
import com.mynote.android.data.entity.ParentCategory
import com.mynote.android.data.entity.SubCategory
import com.mynote.android.ui.base.BaseActivity
import com.mynote.android.ui.edit.EditActivity
import com.mynote.android.ui.main.adapter.NoteItemAdapter
import com.mynote.android.ui.main.adapter.ParentCategoryAdapter
import com.mynote.android.ui.main.adapter.SubCategoryAdapter
import com.mynote.android.ui.main.dialog.CategoryEditDialog
import com.mynote.android.ui.main.dialog.DeletePasswordDialog
import com.mynote.android.ui.main.dialog.MoveSubDialog
import com.mynote.android.ui.settings.SettingsActivity
import com.mynote.android.util.BackgroundHelper
import com.mynote.android.util.Prefs
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : BaseActivity() {

    // ===== Views =====
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var btnSearchToggle: ImageButton
    private lateinit var btnSelectMode: TextView
    private lateinit var searchBar: View
    private lateinit var etSearch: EditText
    private lateinit var btnSearchClear: TextView
    private lateinit var searchSuggestions: View
    private lateinit var rvSearchSuggestions: RecyclerView
    private lateinit var tvSearchHistoryLabel: TextView
    private lateinit var btnClearHistory: TextView
    private lateinit var selectToolbar: View
    private lateinit var btnSelectAll: TextView
    private lateinit var btnExportSelected: TextView
    private lateinit var btnDeleteSelected: TextView
    private lateinit var btnExport: TextView
    private lateinit var btnSort: TextView
    private lateinit var btnPatient: TextView
    private lateinit var btnTools: TextView
    private lateinit var btnSettings: TextView
    private var sortMode = 0 // 0=置顶优先, 1=最近更新, 2=按标题
    private val sortLabels = arrayOf("置顶↑", "时间↓", "标题")
    private lateinit var rvParentCategories: RecyclerView
    private lateinit var rvSubCategories: RecyclerView
    private lateinit var rvNotes: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var fabCreate: FloatingActionButton

    // ===== Data =====
    private val db by lazy { AppDatabase.get(application) }
    private val prefs by lazy { Prefs(this) }
    private var allParents: List<ParentCategory> = emptyList()
    private var allSubs: List<SubCategory> = emptyList()
    private var allNotes: List<Note> = emptyList()
    private val noteCountMap = mutableMapOf<String, Int>()

    // ===== Navigation State =====
    private var currentParentId: String? = null // null = 大主题网格
    private var currentSubId: String? = null     // null = 子主题列表
    private var isSearching = false
    private var isSelectMode = false
    private var lastSearch = ""

    // ===== Adapters =====
    private lateinit var parentAdapter: ParentCategoryAdapter
    private lateinit var subAdapter: SubCategoryAdapter
    private lateinit var noteAdapter: NoteItemAdapter

    // ===== File Picker =====
    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importFile(it) }
    }

    // ===== Export =====
    private var pendingExportNotes: List<Note>? = null

    private val exportDirPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { treeUri ->
            contentResolver.takePersistableUriPermission(treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            getSharedPreferences("mynote_export", MODE_PRIVATE).edit()
                .putString("export_dir_uri", treeUri.toString()).apply()
            pendingExportNotes?.let { doExport(it, treeUri) }
            pendingExportNotes = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initAdapters()
        loadData()
    }

    // ===== Init =====
    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_title)
        btnSearchToggle = findViewById(R.id.btn_search_toggle)
        btnSelectMode = findViewById(R.id.btn_select_mode)
        searchBar = findViewById(R.id.search_bar)

        findViewById<TextView>(R.id.btn_tools)?.setOnClickListener {
            showToolsDialog()
        }
        findViewById<TextView>(R.id.btn_settings).setOnClickListener {
            openSettings()
        }
        findViewById<TextView>(R.id.btn_patient)?.setOnClickListener {
            startActivity(Intent(this, com.mynote.android.ui.patient.PatientListActivity::class.java))
        }
        btnPatient = findViewById(R.id.btn_patient)
        btnTools = findViewById(R.id.btn_tools)
        btnSettings = findViewById(R.id.btn_settings)
        etSearch = findViewById(R.id.et_search)
        selectToolbar = findViewById(R.id.select_toolbar)
        btnSelectAll = findViewById(R.id.btn_select_all)
        btnExportSelected = findViewById(R.id.btn_export_selected)
        btnDeleteSelected = findViewById(R.id.btn_delete_selected)
        btnExport = findViewById(R.id.btn_export)
        btnSort = findViewById(R.id.btn_sort)
        btnExport.setOnClickListener { exportCurrent() }
        btnSort.setOnClickListener { cycleSortMode() }
        rvParentCategories = findViewById(R.id.rv_parent_categories)
        rvSubCategories = findViewById(R.id.rv_sub_categories)
        rvNotes = findViewById(R.id.rv_notes)
        tvEmpty = findViewById(R.id.tv_empty)
        fabCreate = findViewById(R.id.fab_create)
        btnSearchClear = findViewById(R.id.btn_search_clear)
        searchSuggestions = findViewById(R.id.search_suggestions)
        rvSearchSuggestions = findViewById(R.id.rv_search_suggestions)
        tvSearchHistoryLabel = findViewById(R.id.tv_search_history_label)
        btnClearHistory = findViewById(R.id.btn_clear_history)

        btnBack.setOnClickListener { goBack() }
        btnSearchToggle.setOnClickListener { toggleSearch() }
        btnSelectMode.setOnClickListener { toggleSelectMode() }
        btnSelectAll.setOnClickListener { toggleSelectAll() }
        btnExportSelected.setOnClickListener { exportSelected() }
        btnDeleteSelected.setOnClickListener { batchDelete() }
        fabCreate.setOnClickListener { createNote() }
        btnSearchClear.setOnClickListener {
            etSearch.text?.clear()
            btnSearchClear.visibility = View.GONE
        }
        btnClearHistory.setOnClickListener {
            prefs.clearSearchHistory()
            showSearchSuggestions()
        }

        // 搜索建议列表
        rvSearchSuggestions.layoutManager = LinearLayoutManager(this)
        rvSearchSuggestions.adapter = SearchSuggestionAdapter { query ->
            etSearch.setText(query)
            etSearch.setSelection(query.length)
            searchSuggestions.visibility = View.GONE
            prefs.addSearchHistory(query)
            refreshNotes()
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                lastSearch = s?.toString() ?: ""
                btnSearchClear.visibility = if (lastSearch.isNotBlank()) View.VISIBLE else View.GONE
                if (lastSearch.isBlank()) {
                    showSearchSuggestions()
                } else {
                    searchSuggestions.visibility = View.GONE
                    prefs.addSearchHistory(lastSearch)
                }
                refreshNotes()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun initAdapters() {
        // Parent Category Adapter (2 columns grid)
        rvParentCategories.layoutManager = GridLayoutManager(this, 2)
        parentAdapter = ParentCategoryAdapter(
            onItemClick = { selectParent(it) },
            onEditClick = { showEditParentDialog(it) },
            onDeleteClick = { deleteParent(it) },
            onAddClick = { showAddParentDialog() }
        )
        rvParentCategories.adapter = parentAdapter

        // Sub Category Adapter
        rvSubCategories.layoutManager = LinearLayoutManager(this)
        subAdapter = SubCategoryAdapter(
            onItemClick = { selectSub(it) },
            onEditClick = { showEditSubDialog(it) },
            onDeleteClick = { deleteSub(it) },
            onAddClick = { showAddSubDialog() }
        )
        rvSubCategories.adapter = subAdapter

        // Note Adapter
        rvNotes.layoutManager = LinearLayoutManager(this)
        noteAdapter = NoteItemAdapter(
            onItemClick = { openNote(it) },
            onSwipeDelete = { note, position ->
                if (position == -1 && isSelectMode) {
                    noteAdapter.toggleSelection(note.id)
                    updateSelectUI()
                } else {
                    showDeletePasswordDialog { deleteNote(note) }
                }
            },
            onTogglePin = { note ->
                lifecycleScope.launch {
                    db.noteDao().togglePin(note.id)
                    // 更新缓存并刷新列表
                    allNotes = allNotes.map { if (it.id == note.id) it.copy(isPinned = !it.isPinned) else it }
                    refreshNotes()
                }
            }
        )
        rvNotes.adapter = noteAdapter
    }

    // ===== Data Loading =====
    private fun loadData() {
        lifecycleScope.launch {
            allParents = db.categoryDao().getParentCategories()
            val loadedSubs = mutableListOf<SubCategory>()
            for (p in allParents) {
                loadedSubs.addAll(db.categoryDao().getSubCategories(p.id))
            }
            allSubs = loadedSubs
            val loadedNotes = mutableListOf<Note>()
            for (s in allSubs) {
                loadedNotes.addAll(db.noteDao().getNotesBySubCategory(s.id))
            }
            allNotes = loadedNotes
            updateNoteCounts()
            refreshUI()
        }
    }

    private fun updateNoteCounts() {
        noteCountMap.clear()
        for (note in allNotes) {
            val subId = note.subCategoryId
            noteCountMap[subId] = (noteCountMap[subId] ?: 0) + 1
            // 向上汇总到大主题
            val parentId = allSubs.find { it.id == subId }?.parentId
            if (parentId != null) {
                noteCountMap[parentId] = (noteCountMap[parentId] ?: 0) + 1
            }
        }
    }

    // ===== UI Refresh =====
    private fun refreshUI() {
        when {
            isSearching -> {
                rvParentCategories.visibility = View.GONE
                rvSubCategories.visibility = View.GONE
                rvNotes.visibility = View.VISIBLE
                fabCreate.visibility = View.GONE
                btnSelectMode.visibility = View.GONE
                btnExport.visibility = View.GONE
                btnSort.visibility = View.GONE
                btnPatient.visibility = View.GONE
                btnTools.visibility = View.GONE
                btnSettings.visibility = View.GONE
            }
            currentSubId != null -> {
                // 笔记列表层
                rvParentCategories.visibility = View.GONE
                rvSubCategories.visibility = View.GONE
                rvNotes.visibility = View.VISIBLE
                fabCreate.visibility = View.VISIBLE
                btnSelectMode.visibility = View.VISIBLE
                btnExport.visibility = View.VISIBLE
                btnSort.visibility = View.VISIBLE
                btnPatient.visibility = View.GONE
                btnTools.visibility = View.GONE
                btnSettings.visibility = View.GONE
                tvTitle.text = allSubs.find { it.id == currentSubId }?.name ?: "笔记"
                btnBack.visibility = View.VISIBLE
            }
            currentParentId != null -> {
                // 子主题层
                rvParentCategories.visibility = View.GONE
                rvSubCategories.visibility = View.VISIBLE
                rvNotes.visibility = View.GONE
                fabCreate.visibility = View.GONE
                btnSelectMode.visibility = View.GONE
                btnExport.visibility = View.GONE
                btnSort.visibility = View.GONE
                btnPatient.visibility = View.VISIBLE
                btnTools.visibility = View.VISIBLE
                btnSettings.visibility = View.VISIBLE
                tvTitle.text = allParents.find { it.id == currentParentId }?.name ?: "子主题"
                btnBack.visibility = View.VISIBLE
                val subs = allSubs.filter { it.parentId == currentParentId }
                val counts = subs.associate { it.id to (noteCountMap[it.id] ?: 0) }
                val parentColors = allParents.associate { it.id to it.color }
                subAdapter.submitList(subs, counts, parentColors)
            }
            else -> {
                // 大主题网格层
                rvParentCategories.visibility = View.VISIBLE
                rvSubCategories.visibility = View.GONE
                rvNotes.visibility = View.GONE
                fabCreate.visibility = View.GONE
                btnSelectMode.visibility = View.GONE
                btnExport.visibility = View.GONE
                btnSort.visibility = View.GONE
                btnPatient.visibility = View.VISIBLE
                btnTools.visibility = View.VISIBLE
                btnSettings.visibility = View.VISIBLE
                tvTitle.text = "MyNote"
                btnBack.visibility = View.GONE
                val counts = allParents.associate { it.id to (noteCountMap[it.id] ?: 0) }
                parentAdapter.submitList(allParents, counts)
            }
        }
        refreshNotes()
        try { com.mynote.android.ui.widget.MyNoteWidget.refreshAll(this) } catch (_: Exception) {}
        applyBackground()
    }

    private fun refreshNotes() {
        if (isSearching && lastSearch.isNotBlank()) {
            lifecycleScope.launch {
                val results = AppDatabase.get(this@MainActivity).noteDao().searchNotes(lastSearch)
                withContext(Dispatchers.Main) {
                    noteAdapter.searchQuery = lastSearch
                    noteAdapter.submitList(sortNotes(results))
                    tvEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                    applyBackground()
                }
            }
            return
        }
        val notes = if (currentSubId != null) {
            allNotes.filter { it.subCategoryId == currentSubId }
        } else {
            emptyList()
        }
        noteAdapter.searchQuery = ""
        noteAdapter.submitList(sortNotes(notes))
        tvEmpty.visibility = if (notes.isEmpty() && currentSubId != null) View.VISIBLE else View.GONE
        applyBackground()
    }

    private fun sortNotes(list: List<Note>): List<Note> {
        return when (sortMode) {
            1 -> list.sortedByDescending { it.updateTime } // 最近更新
            2 -> list.sortedBy { it.title }                // 按标题
            else -> list.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updateTime }) // 置顶优先
        }
    }

    private fun cycleSortMode() {
        sortMode = (sortMode + 1) % sortLabels.size
        btnSort.text = sortLabels[sortMode]
        refreshNotes()
    }

    // ===== Navigation =====
    private fun selectParent(parent: ParentCategory) {
        currentParentId = parent.id
        currentSubId = null
        isSearching = false
        refreshUI()
    }

    private fun selectSub(sub: SubCategory) {
        currentSubId = sub.id
        isSearching = false
        refreshUI()
    }

    private fun goBack() {
        when {
            isSearching -> toggleSearch()
            currentSubId != null -> {
                currentSubId = null
                refreshUI()
            }
            currentParentId != null -> {
                currentParentId = null
                refreshUI()
            }
            else -> finish()
        }
    }

    // ===== Search =====
    private fun toggleSearch() {
        isSearching = !isSearching
        if (isSearching) {
            searchBar.visibility = View.VISIBLE
            etSearch.requestFocus()
            if (isSelectMode) toggleSelectMode()
            showSearchSuggestions()
        } else {
            searchBar.visibility = View.GONE
            searchSuggestions.visibility = View.GONE
            lastSearch = ""
            etSearch.text?.clear()
            currentParentId = null
            currentSubId = null
        }
        refreshUI()
    }

    // ===== Parent CRUD =====
    private fun showAddParentDialog() {
        CategoryEditDialog("新建大主题", "", "#2196F3") { name, color ->
            lifecycleScope.launch {
                val parent = ParentCategory(
                    id = "parent_${UUID.randomUUID()}",
                    name = name,
                    color = color
                )
                db.categoryDao().insertParentCategory(parent)
                allParents = db.categoryDao().getParentCategories()
                refreshUI()
            }
            Toast.makeText(this, "大主题已创建", Toast.LENGTH_SHORT).show()
        }.show(supportFragmentManager, "addParent")
    }

    private fun showEditParentDialog(parent: ParentCategory) {
        CategoryEditDialog("编辑大主题", parent.name, parent.color) { name, color ->
            lifecycleScope.launch {
                db.categoryDao().updateParentCategory(parent.copy(name = name, color = color))
                allParents = db.categoryDao().getParentCategories()
                refreshUI()
            }
        }.show(supportFragmentManager, "editParent")
    }

    private fun deleteParent(parent: ParentCategory) {
        showDeletePasswordDialog {
            lifecycleScope.launch {
                try {
                    val subs = db.categoryDao().getSubCategories(parent.id)
                    for (sub in subs) {
                        db.categoryDao().deleteSubCategory(sub)
                    }
                    db.categoryDao().deleteParentCategory(parent)
                    if (currentParentId == parent.id) currentParentId = null
                    allParents = db.categoryDao().getParentCategories()
                    loadData()
                    refreshUI()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity,
                        "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ===== Sub CRUD =====
    private fun showAddSubDialog() {
        val parentColor = allParents.find { it.id == currentParentId }?.color ?: "#2196F3"
        CategoryEditDialog("新建子主题", "", parentColor) { name, color ->
            lifecycleScope.launch {
                val sub = SubCategory(
                    id = "sub_${UUID.randomUUID()}",
                    parentId = currentParentId ?: return@launch,
                    name = name,
                    color = color
                )
                db.categoryDao().insertSubCategory(sub)
                allSubs = db.categoryDao().getSubCategories(currentParentId!!)
                refreshUI()
            }
        }.show(supportFragmentManager, "addSub")
    }

    private fun showEditSubDialog(sub: SubCategory) {
        val parentColor = allParents.find { it.id == sub.parentId }?.color ?: "#2196F3"
        val initialColor = sub.color.takeIf { it.isNotBlank() } ?: parentColor
        CategoryEditDialog("编辑子主题", sub.name, initialColor) { name, color ->
            lifecycleScope.launch {
                db.categoryDao().updateSubCategory(sub.copy(name = name, color = color))
                allSubs = db.categoryDao().getSubCategories(currentParentId ?: "")
                refreshUI()
            }
        }.show(supportFragmentManager, "editSub")
    }

    private fun showMoveSubDialog(sub: SubCategory) {
        MoveSubDialog(allParents, currentParentId ?: "") { target ->
            lifecycleScope.launch {
                db.categoryDao().insertSubCategory(sub.copy(parentId = target.id))
                db.categoryDao().deleteSubCategory(sub)
                allSubs = db.categoryDao().getSubCategories(currentParentId ?: "")
                allNotes = allNotes.map { note ->
                    if (note.subCategoryId == sub.id) note.copy(subCategoryId = sub.id) else note
                }.also { updateNoteCounts() }
                refreshUI()
            }
            Toast.makeText(this, "已移动到 ${target.name}", Toast.LENGTH_SHORT).show()
        }.show(supportFragmentManager, "moveSub")
    }

    private fun deleteSub(sub: SubCategory) {
        showDeletePasswordDialog {
            lifecycleScope.launch {
                try {
                    db.categoryDao().deleteSubCategory(sub)
                    if (currentSubId == sub.id) currentSubId = null
                    allSubs = db.categoryDao().getSubCategories(currentParentId ?: "")
                    loadData()
                    refreshUI()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity,
                        "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ===== Note CRUD =====
    private fun createNote() {
        val noteId = UUID.randomUUID().toString()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val note = Note(
            id = noteId,
            subCategoryId = currentSubId ?: "",
            title = "",
            contentText = "",
            updateTime = now,
            createTime = now
        )
        lifecycleScope.launch {
            db.noteDao().insertNote(note)
            allNotes = db.noteDao().getNotesBySubCategory(currentSubId ?: "")
            updateNoteCounts()
            openEditPage(noteId)
        }
    }

    private fun openNote(note: Note) {
        openEditPage(note.id)
    }

    private fun openEditPage(noteId: String) {
        startActivity(Intent(this, EditActivity::class.java).apply {
            putExtra("noteId", noteId)
        })
    }

    private fun deleteNote(note: Note) {
        lifecycleScope.launch {
            db.noteDao().moveToTrash(note.id, System.currentTimeMillis())
            allNotes = allNotes.filter { it.id != note.id }
            updateNoteCounts()
            refreshNotes()
            refreshUI()
            Toast.makeText(this@MainActivity, "已移入回收站", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeletePasswordDialog(onVerify: () -> Unit) {
        DeletePasswordDialog(onVerify).show(supportFragmentManager, "deletePwd")
    }

    // ===== Select Mode =====
    private fun toggleSelectMode() {
        isSelectMode = !isSelectMode
        noteAdapter.setSelectMode(isSelectMode)
        selectToolbar.visibility = if (isSelectMode) View.VISIBLE else View.GONE
        btnSelectMode.text = if (isSelectMode) "取消" else "选择"
        updateSelectUI()
    }

    private fun toggleSelectAll() {
        if (noteAdapter.getSelectedIds().isNotEmpty()) {
            noteAdapter.clearSelection()
        } else {
            noteAdapter.selectAll()
        }
        updateSelectUI()
    }

    private fun updateSelectUI() {
        val count = noteAdapter.getSelectedIds().size
        btnSelectAll.text = if (count > 0) "取消全选($count)" else "全选"
        btnExportSelected.visibility = if (count > 0) View.VISIBLE else View.GONE
        btnDeleteSelected.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private fun exportSelected() {
        val selected = noteAdapter.getSelectedIds()
        val notes = allNotes.filter { selected.contains(it.id) }
        if (notes.isEmpty()) return
        pendingExportNotes = notes
        startExport()
        toggleSelectMode()
    }

    private fun batchDelete() {
        val selected = noteAdapter.getSelectedIds()
        if (selected.isEmpty()) return
        showDeletePasswordDialog {
            lifecycleScope.launch {
                val now = System.currentTimeMillis()
                selected.forEach { id ->
                    db.noteDao().moveToTrash(id, now)
                }
                allNotes = allNotes.filter { it.id !in selected }
                updateNoteCounts()
                refreshNotes()
                toggleSelectMode()
                Toast.makeText(this@MainActivity, "已移入回收站", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportCurrent() {
        if (currentSubId == null) return
        val notes = allNotes.filter { it.subCategoryId == currentSubId }
        if (notes.isEmpty()) {
            Toast.makeText(this, "没有笔记可导出", Toast.LENGTH_SHORT).show()
            return
        }
        pendingExportNotes = notes
        startExport()
    }

    private fun startExport() {
        val prefs = getSharedPreferences("mynote_export", MODE_PRIVATE)
        val uriStr = prefs.getString("export_dir_uri", null)
        if (uriStr != null) {
            try {
                val uri = Uri.parse(uriStr)
                val perms = contentResolver.persistedUriPermissions
                if (perms.any { it.uri == uri && it.isWritePermission }) {
                    pendingExportNotes?.let { doExport(it, uri) }
                    pendingExportNotes = null
                    return
                }
            } catch (e: Exception) {}
        }
        Toast.makeText(this, "请选择导出保存的文件夹", Toast.LENGTH_SHORT).show()
        exportDirPicker.launch(null)
    }

    private fun doExport(notes: List<Note>, dirUri: Uri) {
        lifecycleScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val folderName = "MyNote_${timestamp}"

                val treeDir = DocumentFile.fromTreeUri(this@MainActivity, dirUri)
                    ?: throw Exception("无法访问选择的文件夹")
                val exportDir = treeDir.createDirectory(folderName)
                    ?: throw Exception("无法创建文件夹，请检查权限")

                // 1. 生成 HTML
                val html = buildHtmlExport(notes)
                val htmlFile = exportDir.createFile("text/html", "notes.html")
                    ?: throw Exception("无法创建 notes.html")
                contentResolver.openOutputStream(htmlFile.uri)?.use { os ->
                    os.write(html.toByteArray(Charsets.UTF_8))
                }

                // 2. 复制附件
                val targetFilesDir = exportDir.createDirectory("files")
                    ?: throw Exception("无法创建 files 目录")
                var attachmentCount = 0
                for (note in notes) {
                    val srcDir = File(this@MainActivity.filesDir, "notes/${note.id}")
                    if (srcDir.exists() && srcDir.isDirectory) {
                        val noteDir = targetFilesDir.createDirectory(note.id) ?: continue
                        srcDir.listFiles()?.forEach { srcFile: File ->
                            if (srcFile.isFile) {
                                val ext = srcFile.extension.lowercase()
                                val mime = when (ext) {
                                    "mp4", "3gp", "mov", "avi" -> "video/*"
                                    "jpg", "jpeg", "png", "gif", "webp" -> "image/*"
                                    "pdf" -> "application/pdf"
                                    "doc", "docx" -> "application/msword"
                                    "xls", "xlsx" -> "application/vnd.ms-excel"
                                    "ppt", "pptx" -> "application/vnd.ms-powerpoint"
                                    "mp3", "wav", "amr" -> "audio/*"
                                    else -> "*/*"
                                }
                                val destFile = noteDir.createFile(mime, srcFile.name)
                                if (destFile != null) {
                                    contentResolver.openOutputStream(destFile.uri)?.use { os ->
                                        srcFile.inputStream().use { srcInput -> srcInput.copyTo(os) }
                                    }
                                    attachmentCount++
                                }
                            }
                        }
                    }
                }

                Toast.makeText(this@MainActivity,
                    "导出成功！\n${folderName}\n共 ${notes.size} 篇笔记，$attachmentCount 个附件",
                    Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                getSharedPreferences("mynote_export", MODE_PRIVATE).edit().remove("export_dir_uri").apply()
            }
        }
    }

    private suspend fun buildHtmlExport(notes: List<Note>): String {
        val dao = db.noteDao()
        val sb = StringBuilder()
        sb.appendLine("""<!DOCTYPE html>
<html><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>MyNote 导出</title>
<style>
body{font-family:sans-serif;max-width:900px;margin:0 auto;padding:20px;color:#333;background:#fafafa}
h1{color:#4CAF50;text-align:center;margin-bottom:8px}
.subtitle{text-align:center;color:#999;margin-bottom:32px}
h2{color:#333;border-bottom:2px solid #4CAF50;padding-bottom:8px;margin-top:40px}
.meta{color:#999;font-size:12px;margin:4px 0 16px}
.content{max-width:100%}
.content img{max-width:100%;height:auto;border-radius:8px;margin:8px 0;box-shadow:0 2px 8px rgba(0,0,0,0.1)}
.content video{max-width:100%;border-radius:8px;margin:8px 0}
.content .voice-msg{display:inline-block;background:#95EC69;color:#333;padding:8px 12px;border-radius:16px;margin:4px 0}
.content .shape-inline{display:inline-block;width:40px;height:40px;vertical-align:middle;margin:4px}
.note{background:white;border-radius:12px;padding:24px;margin-bottom:24px;box-shadow:0 2px 12px rgba(0,0,0,0.08)}
.media-file{display:flex;align-items:center;gap:12px;padding:12px 16px;margin:8px 0;border:1px solid #e0e0e0;border-radius:8px;background:#f9f9f9}
.file-icon{font-size:28px}
.file-name{font-weight:600;color:#333}
.file-hint{font-size:11px;color:#999}
</style>
</head><body>
<h1>MyNote 导出</h1>
<p class="subtitle">共 ${notes.size} 篇笔记 | ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}</p>
""")
        for (note in notes) {
            val title = note.title.ifEmpty { "无标题" }
            val items = dao.getContentItems(note.id)
            val htmlItem = items.firstOrNull { it.type == "html" }
            var body = htmlItem?.content ?: note.contentText.replace("\n", "<br>")
            body = embedMediaInHtml(body, note.id)
            sb.appendLine("<div class=\"note\">")
            sb.appendLine("<h2>$title</h2>")
            sb.appendLine("<div class=\"meta\">更新: ${note.updateTime} | 创建: ${note.createTime}</div>")
            sb.appendLine("<div class=\"content\">$body</div>")
            sb.appendLine("</div>")
        }
        sb.appendLine("</body></html>")
        return sb.toString()
    }

    /** 处理 HTML 中的媒体引用：图片内嵌 Base64，视频/PDF/音频显示为文件链接 */
    private fun embedMediaInHtml(html: String, noteId: String): String {
        val noteDir = File(filesDir, "notes/$noteId")
        if (!noteDir.exists()) return html
        var result = html

        // 匹配 src="...notes/noteId/..." 的图片引用
        val imgPattern = Regex("""src=["']([^"']*notes[/\\]$noteId[/\\][^"']+\.(jpg|jpeg|png|gif|webp))["']""", RegexOption.IGNORE_CASE)
        result = result.replace(imgPattern) { match ->
            val path = match.groupValues[1]
            val file = if (path.startsWith("/")) File(path) else File(noteDir, File(path).name)
            if (file.exists() && file.length() < 5 * 1024 * 1024) {
                try {
                    val bytes = file.readBytes()
                    val ext = file.extension.lowercase()
                    val mime = when (ext) {
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        "gif" -> "image/gif"
                        "webp" -> "image/webp"
                        else -> "image/jpeg"
                    }
                    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    "src=\"data:$mime;base64,$b64\""
                } catch (e: Exception) { match.value }
            } else match.value
        }

        // 处理 media-card（PDF/Office/视频文件卡片）
        val cardPattern = Regex(
            """<div[^>]*class=["']media-card["'][^>]*data-path=["']([^"']+)["'][^>]*>.*?</div>\s*</div>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        result = result.replace(cardPattern) { match ->
            val path = match.groupValues[1]
            val fileName = File(path).name
            if (path.contains(noteId)) {
                val relPath = "files/$noteId/$fileName"
                val ext = fileName.substringAfterLast(".").lowercase()
                val icon = when (ext) {
                    "mp4", "3gp", "avi", "mov" -> "\uD83C\uDFAC"
                    "pdf" -> "\uD83D\uDCC4"
                    "doc", "docx" -> "\uD83D\uDCDD"
                    "xls", "xlsx" -> "\uD83D\uDCCA"
                    "ppt", "pptx" -> "\uD83D\uDCCB"
                    else -> "\uD83D\uDCCE"
                }
                val label = when (ext) {
                    "mp4", "3gp", "avi", "mov" -> "视频文件"
                    in listOf("mp3", "wav", "3gp", "amr", "m4a") -> "音频文件"
                    else -> "附件"
                }
                """<div class="media-file">
<div class="file-icon">$icon</div>
<div class="file-name">$fileName</div>
<div class="file-hint">$label（导出包中有原始文件）</div>
</div>"""
            } else match.value
        }

        // 处理 <video> 标签
        val videoPattern = Regex("""<video[^>]*src=["']([^"']+)["'][^>]*>.*?</video>""", RegexOption.DOT_MATCHES_ALL)
        result = result.replace(videoPattern) { match ->
            val src = match.groupValues[1]
            val fileName = File(src).name
            if (src.contains(noteId)) {
                val relPath = "files/$noteId/$fileName"
                """<div class="media-file">
<div class="file-icon">\uD83C\uDFAC</div>
<div class="file-name">$fileName</div>
<div class="file-hint">视频文件（导出包中有原始文件）</div>
</div>"""
            } else match.value
        }

        // 处理语音消息
        val voicePattern = Regex("""<div[^>]*class=["']voice-msg["'][^>]*data-path=["']([^"']+\.(3gp|amr|mp3|wav|m4a))["'][^>]*>.*?</div>""", setOf(RegexOption.DOT_MATCHES_ALL))
        result = result.replace(voicePattern) { match ->
            val path = match.groupValues[1]
            val fileName = File(path).name
            if (path.contains(noteId)) {
                val relPath = "files/$noteId/$fileName"
                """<div class="media-file">
<div class="file-icon">\uD83C\uDFA4</div>
<div class="file-name">$fileName</div>
<div class="file-hint">语音（导出包中有原始文件）</div>
</div>"""
            } else match.value
        }

        return result
    }

    // ===== Import =====
    private fun importFile(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val raw = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()

                if (raw.isBlank()) {
                    Toast.makeText(this@MainActivity, "文件为空", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                var title = ""
                var content = raw

                // HTML 检测
                if (raw.trimStart().startsWith("<") && raw.lowercase().contains("<html")) {
                    val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(raw)
                    title = titleMatch?.groupValues?.get(1) ?: "导入笔记"
                    val bodyText = Regex("<body[^>]*>(.*?)</body>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(raw)
                    content = bodyText?.groupValues?.get(1)?.replace(Regex("<[^>]+>"), " ")?.trim() ?: raw
                } else {
                    title = uri.lastPathSegment?.replace(Regex("\\.[^.]+$"), "") ?: "导入笔记"
                }

                createImportedNote(title, content)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createImportedNote(title: String, content: String) {
        val noteId = UUID.randomUUID().toString()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val note = Note(
            id = noteId,
            subCategoryId = currentSubId ?: "",
            title = title,
            contentText = content,
            updateTime = now,
            createTime = now
        )
        lifecycleScope.launch {
            db.noteDao().insertNote(note)
            allNotes = db.noteDao().getNotesBySubCategory(currentSubId ?: "")
            updateNoteCounts()
            refreshNotes()
            refreshUI()
            Toast.makeText(this@MainActivity, "已导入: $title", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSettings() {
        val prefs = getSharedPreferences("mynote_prefs", MODE_PRIVATE)
        val pwdEnabled = prefs.getBoolean("password_enabled", false)
        val pwd = prefs.getString("password", null)

        if (!pwdEnabled || pwd.isNullOrEmpty()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_set_password, null)
        val etPwd = view.findViewById<EditText>(R.id.et_password)
        view.findViewById<TextView>(R.id.tv_dialog_title).visibility = View.GONE
        view.findViewById<TextView>(R.id.tv_dialog_subtitle).visibility = View.GONE
        etPwd.hint = ""

        val dialog = AlertDialog.Builder(this, R.style.RoundedDialog)
            .setView(view)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.visibility = View.GONE
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.GONE
        }

        // 输入位数匹配密码长度 → 自动验证
        val pwdLen = pwd.length
        etPwd.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s != null && s.length == pwdLen) {
                    if (s.toString() == pwd) {
                        dialog.dismiss()
                        lockManager.onUnlocked()
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    } else {
                        dialog.dismiss()
                    }
                }
            }
        })

        dialog.show()
        etPwd.requestFocus()
    }

    override fun onBackPressed() {
        if (isSearching || currentSubId != null || currentParentId != null) {
            goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        if (prefs.needsRefresh) {
            prefs.needsRefresh = false
            loadData()
        }
        applyBackground()
    }

    private fun applyBackground() {
        val root = findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.root_coordinator)

        when {
            // 主页：大主题网格 → 跟随大主题卡片颜色渐变
            currentParentId == null && currentSubId == null -> {
                val firstParent = allParents.firstOrNull()
                root.background = if (firstParent != null) {
                    BackgroundHelper.getCategoryBackground(firstParent.color)
                } else {
                    android.graphics.drawable.ColorDrawable(
                        androidx.core.content.ContextCompat.getColor(this, R.color.bg_main))
                }
            }
            // 二级主页：子主题笔记列表 → 跟随子主题颜色渐变
            currentSubId != null -> {
                val sub = allSubs.find { it.id == currentSubId }
                if (sub != null) {
                    root.background = BackgroundHelper.getCategoryBackground(sub.color)
                }
            }
            // 中间层：大主题下的子主题列表 → 跟随大主题颜色渐变
            else -> {
                val parent = allParents.find { it.id == currentParentId }
                if (parent != null) {
                    root.background = BackgroundHelper.getCategoryBackground(parent.color)
                }
            }
        }
    }

    // ===== 工具面板 =====
    private fun showToolsDialog() {
        val items = listOf(
            "📚 知识库" to listOf(
                "疾病速查 (960种)" to { startSettings("disease") },
                "用药参考 (367种)" to { startSettings("drug") },
                "检验参考值 (155项)" to { startSettings("lab") },
                "影像征象 (246种)" to { startSettings("imaging") },
                "心电图速查 (51种)" to { startSettings("ecg") },
                "指南速查 (100+篇)" to { startSettings("guide") },
                "临床路径 (18条)" to { startSettings("pathway") },
                "急救流程 (11项)" to { startSettings("emergency") },
                "抗菌药物选药 (25项)" to { startSettings("abx") },
                "输血指征 (16项)" to { startSettings("transfusion") },
                "中毒与解毒 (14项)" to { startSettings("tox") },
                "儿童生长发育/用药" to { startSettings("peds") },
                "临床量表 (21项)" to { startSettings("scores") },
            ),
            "🧮 临床工具" to listOf(
                "医学计算器 (38项)" to { startSettings("calc") },
                "输液速度计算" to { startSettings("ivdrip") },
                "体表面积 BSA" to { startSettings("bsa") },
                "儿童剂量速算" to { startSettings("pedidose") },
                "创伤评分 ISS/RTS" to { startSettings("trauma") },
                "疼痛评估 NRS" to { startSettings("pain") },
                "妊娠用药分级" to { startSettings("pregdrug") },
                "ABG 血气判读" to { startSettings("abg") },
                "药物相互作用" to { startSettings("drugInteract") },
                "AI 读化验单" to { startSettings("aiLab") },
            ),
            "📌 数据管理" to listOf(
                "标签管理" to { showTagsDialog() },
                "会议记录" to { startActivity(android.content.Intent(this@MainActivity, com.mynote.android.ui.meeting.MeetingActivity::class.java)) },
            ),
        )
        var dialog: AlertDialog? = null
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 8, 20, 0) }
        // 全局搜索
        val etSearch = EditText(this).apply {
            hint = "🔍 搜索所有知识库和工具..."; setSingleLine(); textSize = 14f
            setPadding(12, 12, 12, 12); setBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 4, 0, 8) }
        }
        root.addView(etSearch)
        val scroll = ScrollView(this)
        val resultContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        root.addView(resultContainer)
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(container)
        root.addView(scroll)
        // 最近使用
        val recentKeys = getSharedPreferences("mynote_tools", Context.MODE_PRIVATE).getString("recent", "")?.split(",")?.filter { it.isNotBlank() }?.take(5) ?: emptyList()
        if (recentKeys.isNotEmpty()) {
            val recentLabel = TextView(this).apply {
                text = "🕐 最近使用"; textSize = 13f; setTextColor(Color.parseColor("#EF6C00"))
                setPadding(16, 12, 16, 8); typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            container.addView(recentLabel)
            for (key in recentKeys) {
                val label = findLabelByKey(key)
                val tv = TextView(this).apply {
                    text = label; textSize = 14f; setPadding(24, 10, 24, 10)
                    setTextColor(Color.DKGRAY)
                    setOnClickListener { dialog?.dismiss(); startSettings(key) }
                }
                container.addView(tv)
            }
            container.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(16, 8, 16, 4) }; setBackgroundColor(Color.parseColor("#E0E0E0")) })
        }
        // 默认第一个分组展开，其余收起
        var firstGroup = true
        for ((section, subs) in items) {
            val groupContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = if (firstGroup) View.VISIBLE else View.GONE }
            val header = TextView(this).apply {
                text = if (firstGroup) "▼ $section" else "▶ $section"
                textSize = 14f; setTextColor(Color.parseColor("#1976D2"))
                setPadding(16, 16, 16, 8); typeface = android.graphics.Typeface.DEFAULT_BOLD
                setOnClickListener {
                    if (groupContent.visibility == View.VISIBLE) {
                        groupContent.visibility = View.GONE; text = "▶ $section"
                    } else {
                        groupContent.visibility = View.VISIBLE; text = "▼ $section"
                    }
                }
            }
            container.addView(header)
            for ((label, action) in subs) {
                val tv = TextView(this).apply {
                    text = label; textSize = 15f; setPadding(24, 12, 24, 12)
                    setTextColor(Color.DKGRAY)
                    setOnClickListener { dialog?.dismiss(); action() }
                }
                groupContent.addView(tv)
            }
            container.addView(groupContent)
            firstGroup = false
        }
        dialog = AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("🛠 工具").setView(root as android.view.View)
            .setNegativeButton("关闭", null).create()
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), (resources.displayMetrics.heightPixels * 0.78).toInt())

        // 搜索逻辑
        val searchIndex = buildGlobalSearchIndex()
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim().lowercase()
                if (q.length < 1) {
                    scroll.visibility = View.VISIBLE; resultContainer.visibility = View.GONE; resultContainer.removeAllViews(); return
                }
                scroll.visibility = View.GONE; resultContainer.visibility = View.VISIBLE; resultContainer.removeAllViews()
                val results = searchIndex.filter {
                    it.first.lowercase().contains(q) || it.second.lowercase().contains(q) ||
                    com.mynote.android.util.PinyinUtil.toInitials(it.first).contains(q)
                }.take(20)
                if (results.isEmpty()) {
                    resultContainer.addView(TextView(this@MainActivity).apply {
                        text = "未找到匹配结果"; setTextColor(Color.GRAY); textSize = 14f; setPadding(16, 12, 16, 12)
                    })
                    return
                }
                val header = TextView(this@MainActivity).apply {
                    text = "找到 ${results.size} 条结果"; textSize = 12f; setTextColor(Color.parseColor("#1976D2"))
                    setPadding(16, 8, 16, 8); typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                resultContainer.addView(header)
                for ((label, route) in results) {
                    val tv = TextView(this@MainActivity).apply {
                        text = label; textSize = 14f; setPadding(20, 12, 20, 12); setTextColor(Color.DKGRAY)
                        setOnClickListener { dialog?.dismiss(); startSettings(route) }
                    }
                    resultContainer.addView(tv)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, af: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    /** 构建全局搜索索引: (显示标签, 路由key) */
    private fun buildGlobalSearchIndex(): List<Pair<String, String>> {
        val idx = mutableListOf<Pair<String, String>>()
        // 知识库
        for (d in com.mynote.android.util.DiseaseReference.getAll()) idx.add("[疾病] ${d.name}" to "disease")
        for (d in com.mynote.android.util.DrugReference.getAll()) idx.add("[药品] ${d.name}" to "drug")
        for (d in com.mynote.android.util.LabReference.all) idx.add("[检验] ${d.name}" to "lab")
        for (d in com.mynote.android.util.ImagingReference.all) idx.add("[影像] ${d.title}" to "imaging")
        for (d in com.mynote.android.util.ECGReference.all) idx.add("[ECG] ${d.title}" to "ecg")
        for (d in com.mynote.android.util.GuidelineLibrary.all) idx.add("[指南] ${d.title}" to "guide")
        for (d in com.mynote.android.util.ClinicalPathways.pathways) idx.add("[路径] ${d.title}" to "pathway")
        for (d in com.mynote.android.util.EmergencyProcedures.all) idx.add("[急救] ${d.title}" to "emergency")
        for (d in com.mynote.android.util.AntibioticGuide.all) idx.add("[抗菌] ${d.title}" to "abx")
        for (d in com.mynote.android.util.TransfusionGuide.all) idx.add("[输血] ${d.title}" to "transfusion")
        for (d in com.mynote.android.util.ToxicologyRef.all) idx.add("[中毒] ${d.agent}" to "tox")
        for (d in com.mynote.android.util.PediatricGrowth.all) idx.add("[儿科] ${d.title}" to "peds")
        for (d in com.mynote.android.util.ClinicalScores.all) idx.add("[量表] ${d.title}" to "scores")
        // 工具
        idx.add("[工具] 输液速度计算" to "ivdrip"); idx.add("[工具] 体表面积 BSA" to "bsa")
        idx.add("[工具] 儿童剂量速算" to "pedidose"); idx.add("[工具] 创伤评分 ISS/RTS" to "trauma")
        idx.add("[工具] 疼痛评估 NRS" to "pain"); idx.add("[工具] 妊娠用药分级" to "pregdrug")
        idx.add("[工具] ABG 血气判读" to "abg"); idx.add("[工具] 药物相互作用" to "drugInteract")
        idx.add("[工具] AI 读化验单" to "aiLab"); idx.add("[工具] 医学计算器" to "calc")
        return idx
    }

    private fun startSettings(target: String) {
        // 记录最近使用
        val sp = getSharedPreferences("mynote_tools", Context.MODE_PRIVATE)
        val recent = sp.getString("recent", "")?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
        recent.remove(target); recent.add(0, target)
        if (recent.size > 5) recent.removeAt(recent.size - 1)
        sp.edit().putString("recent", recent.joinToString(",")).apply()
        val intent = Intent(this, com.mynote.android.ui.settings.SettingsActivity::class.java)
        intent.putExtra("open", target)
        startActivity(intent)
    }

    private fun findLabelByKey(key: String): String = when (key) {
        "disease" -> "[疾病] 疾病速查"; "drug" -> "[药品] 用药参考"; "lab" -> "[检验] 检验参考值"
        "imaging" -> "[影像] 影像征象"; "ecg" -> "[ECG] 心电图速查"; "guide" -> "[指南] 指南速查"
        "pathway" -> "[路径] 临床路径"; "emergency" -> "[急救] 急救流程"; "abx" -> "[抗菌] 抗菌选药"
        "transfusion" -> "[输血] 输血指征"; "tox" -> "[中毒] 中毒解毒"; "peds" -> "[儿科] 儿童生长"
        "scores" -> "[量表] 临床量表"; "calc" -> "[工具] 医学计算器"; "ivdrip" -> "[工具] 输液速度"
        "bsa" -> "[工具] 体表面积"; "pedidose" -> "[工具] 儿童剂量"; "trauma" -> "[工具] 创伤评分"
        "pain" -> "[工具] 疼痛评估"; "pregdrug" -> "[工具] 妊娠用药"; "abg" -> "[工具] ABG血气"
        "drugInteract" -> "[工具] 药物相互作用"; "aiLab" -> "[工具] AI化验单"
        else -> "[工具] $key"
    }

    // ===== 标签管理 =====
    private fun showTagsDialog() {
        lifecycleScope.launch {
            val rawTags = withContext(Dispatchers.IO) { db.noteDao().getAllTags() }
            val tagCount = mutableMapOf<String, Int>()
            rawTags.forEach { tagsStr ->
                tagsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                    tagCount[tag] = (tagCount[tag] ?: 0) + 1
                }
            }
            val sortedTags = tagCount.entries.sortedByDescending { it.value }

            if (sortedTags.isEmpty()) {
                AlertDialog.Builder(this@MainActivity, R.style.RoundedDialog)
                    .setTitle("标签管理")
                    .setMessage("暂无标签\n\n在笔记编辑页可添加标签（逗号分隔）")
                    .setPositiveButton("好的", null).show()
                return@launch
            }

            val items = sortedTags.map { "${it.key}  (${it.value})" }.toTypedArray()
            AlertDialog.Builder(this@MainActivity, R.style.RoundedDialog)
                .setTitle("标签管理 (${sortedTags.size})")
                .setItems(items) { _, i ->
                    val tag = sortedTags[i].key
                    showTagActionsDialog(tag, sortedTags[i].value)
                }
                .setNegativeButton("关闭", null)
                .show()
        }
    }

    private fun showTagActionsDialog(tag: String, count: Int) {
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("「$tag」($count 篇笔记)")
            .setItems(arrayOf("筛选此标签", "重命名", "删除")) { _, which ->
                when (which) {
                    0 -> filterByTag(tag)
                    1 -> showRenameTagDialog(tag)
                    2 -> showDeleteTagConfirmDialog(tag, count)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun filterByTag(tag: String) {
        lifecycleScope.launch {
            val notes = withContext(Dispatchers.IO) { db.noteDao().getNotesByTag(tag) }
            if (notes.isEmpty()) {
                Toast.makeText(this@MainActivity, "无匹配笔记", Toast.LENGTH_SHORT).show()
                return@launch
            }
            currentSubId = null
            currentParentId = null
            isSearching = false
            noteAdapter.searchQuery = ""
            noteAdapter.submitList(sortNotes(notes))
            rvParentCategories.visibility = View.GONE
            rvSubCategories.visibility = View.GONE
            rvNotes.visibility = View.VISIBLE
            tvEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            tvTitle.text = "标签: $tag"
            btnBack.visibility = View.VISIBLE
            Toast.makeText(this@MainActivity, "标签「$tag」: ${notes.size} 篇", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRenameTagDialog(oldTag: String) {
        val et = EditText(this).apply {
            setText(oldTag); hint = "新标签名"
            setPadding(32, 16, 32, 16)
        }
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("重命名标签")
            .setView(et)
            .setPositiveButton("确认") { _, _ ->
                val newTag = et.text.toString().trim().ifEmpty { oldTag }
                if (newTag == oldTag) return@setPositiveButton
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val notes = db.noteDao().getNotesByTag(oldTag)
                        notes.forEach { note ->
                            val tags = note.tags.split(",").map { it.trim() }
                            val updated = tags.joinToString(",") { if (it == oldTag) newTag else it }
                            db.noteDao().updateNote(note.copy(tags = updated))
                        }
                    }
                    Toast.makeText(this@MainActivity, "已重命名「$oldTag」→「$newTag」", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null).show()
    }

    private fun showDeleteTagConfirmDialog(tag: String, count: Int) {
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("删除标签")
            .setMessage("确定删除标签「$tag」吗？将从 $count 篇笔记中移除此标签。笔记本身不会被删除。")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val notes = db.noteDao().getNotesByTag(tag)
                        notes.forEach { note ->
                            val tags = note.tags.split(",").map { it.trim() }.filter { it != tag && it.isNotBlank() }
                            db.noteDao().updateNote(note.copy(tags = tags.joinToString(",")))
                        }
                    }
                    Toast.makeText(this@MainActivity, "标签「$tag」已删除", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null).show()
    }

    // ===== 搜索建议 =====
    private fun showSearchSuggestions() {
        val history = prefs.searchHistory
        if (history.isEmpty()) {
            searchSuggestions.visibility = View.GONE
            return
        }
        searchSuggestions.visibility = View.VISIBLE
        tvSearchHistoryLabel.text = "搜索历史"
        (rvSearchSuggestions.adapter as? SearchSuggestionAdapter)?.submitList(history)
        btnClearHistory.visibility = View.VISIBLE
    }

    inner class SearchSuggestionAdapter(
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<SearchSuggestionAdapter.VH>() {

        private var items: List<String> = emptyList()

        fun submitList(list: List<String>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                textSize = 14f
                setTextColor(getColor(com.mynote.android.R.color.text_secondary))
                setPadding(40, 12, 16, 12)
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.tv.text = items[position]
            holder.tv.setOnClickListener { onClick(items[position]) }
        }

        override fun getItemCount() = items.size

        inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)
    }
}
