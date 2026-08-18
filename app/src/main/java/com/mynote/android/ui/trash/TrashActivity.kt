package com.mynote.android.ui.trash

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mynote.android.R
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrashActivity : AppCompatActivity() {

    private lateinit var rvTrash: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvHint: TextView
    private lateinit var adapter: TrashAdapter
    private val db by lazy { AppDatabase.get(this) }
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        rvTrash = findViewById(R.id.rv_trash)
        tvEmpty = findViewById(R.id.tv_trash_empty)
        tvHint = findViewById(R.id.tv_trash_hint)

        findViewById<TextView>(R.id.btn_trash_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btn_empty_trash).setOnClickListener { confirmEmptyTrash() }

        adapter = TrashAdapter(
            onRestore = { note -> restoreNote(note) },
            onDelete = { note -> permanentlyDelete(note) }
        )
        rvTrash.layoutManager = LinearLayoutManager(this)
        rvTrash.adapter = adapter

        loadTrash()
    }

    private fun loadTrash() {
        lifecycleScope.launch {
            val notes = withContext(Dispatchers.IO) { db.noteDao().getTrashedNotes() }
            adapter.submitList(notes)
            tvEmpty.visibility = if (notes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            rvTrash.visibility = if (notes.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            tvHint.visibility = if (notes.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        }
    }

    private fun restoreNote(note: Note) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { db.noteDao().restoreFromTrash(note.id) }
            Toast.makeText(this@TrashActivity, "已恢复", Toast.LENGTH_SHORT).show()
            loadTrash()
        }
    }

    private fun permanentlyDelete(note: Note) {
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("彻底删除")
            .setMessage("「${note.title}」将被永久删除，无法恢复")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db.noteDao().deleteNoteById(note.id)
                        try { File(filesDir, "notes/${note.id}").deleteRecursively() } catch (_: Exception) {}
                    }
                    loadTrash()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmEmptyTrash() {
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("清空回收站")
            .setMessage("回收站中所有笔记将被永久删除，无法恢复")
            .setPositiveButton("清空") { _, _ ->
                lifecycleScope.launch {
                    val notes = withContext(Dispatchers.IO) { db.noteDao().getTrashedNotes() }
                    withContext(Dispatchers.IO) {
                        db.noteDao().emptyTrash()
                        notes.forEach { note ->
                            try { File(filesDir, "notes/${note.id}").deleteRecursively() } catch (_: Exception) {}
                        }
                    }
                    Toast.makeText(this@TrashActivity, "回收站已清空", Toast.LENGTH_SHORT).show()
                    loadTrash()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 回收站列表适配器
    inner class TrashAdapter(
        private val onRestore: (Note) -> Unit,
        private val onDelete: (Note) -> Unit
    ) : RecyclerView.Adapter<TrashAdapter.VH>() {

        private var notes: List<Note> = emptyList()

        fun submitList(list: List<Note>) {
            notes = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_trash_note, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(notes[position])
        }

        override fun getItemCount() = notes.size

        inner class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
            private val tvTitle = view.findViewById<TextView>(R.id.tv_trash_title)
            private val tvPreview = view.findViewById<TextView>(R.id.tv_trash_preview)
            private val tvTime = view.findViewById<TextView>(R.id.tv_trash_time)
            private val tvRemaining = view.findViewById<TextView>(R.id.tv_trash_remaining)
            private val btnRestore = view.findViewById<TextView>(R.id.btn_trash_restore)
            private val btnDelete = view.findViewById<TextView>(R.id.btn_trash_delete)

            fun bind(note: Note) {
                tvTitle.text = note.title.ifEmpty { "无标题" }
                tvPreview.text = note.contentText.take(80).replace(Regex("<[^>]+>"), " ").trim().ifEmpty { "无内容" }
                tvTime.text = "删除时间: ${note.updateTime}"

                // 计算剩余天数
                val now = System.currentTimeMillis()
                val trashAge = now - note.trashedAt
                val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
                val remainingMs = thirtyDaysMs - trashAge
                val remainingDays = (remainingMs / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                tvRemaining.text = if (remainingDays > 0) "剩余 ${remainingDays} 天" else "即将清理"

                btnRestore.setOnClickListener { onRestore(note) }
                btnDelete.setOnClickListener { onDelete(note) }
            }
        }
    }
}
