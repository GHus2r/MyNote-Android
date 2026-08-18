package com.mynote.android.ui.main.adapter

import android.animation.ObjectAnimator
import android.graphics.Color
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mynote.android.R
import com.mynote.android.data.entity.Note
import kotlin.math.abs

class NoteItemAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onSwipeDelete: (Note, Int) -> Unit,
    private val onTogglePin: (Note) -> Unit
) : RecyclerView.Adapter<NoteItemAdapter.VH>() {

    private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(old: Note, new: Note) = old.id == new.id
        override fun areContentsTheSame(old: Note, new: Note) =
            old.title == new.title && old.contentText == new.contentText &&
            old.updateTime == new.updateTime && old.isPinned == new.isPinned &&
            old.tags == new.tags && old.isTodo == new.isTodo && old.sortOrder == new.sortOrder
        override fun getChangePayload(old: Note, new: Note): Any? {
            if (old.isPinned != new.isPinned || old.tags != new.tags || old.isTodo != new.isTodo) {
                return PAYLOAD_META
            }
            return null
        }
    })

    companion object {
        private const val PAYLOAD_META = 1
        private const val PAYLOAD_SELECT_MODE = 2
    }
    private val notes: List<Note> get() = differ.currentList

    private var isSelectMode = false
    private val selectedIds = mutableSetOf<String>()
    private var lastSwipedContent: View? = null
    private var lastSwipedDelete: View? = null
    private val SWIPE_THRESHOLD_DP = 30f
    private val SWIPE_BTN_DP = 64f
    private val SWIPE_WIDTH_DP = 128f

    var searchQuery = ""

    fun submitList(list: List<Note>) {
        differ.submitList(list.toList())
    }

    fun setSelectMode(enable: Boolean) {
        isSelectMode = enable
        if (!enable) {
            selectedIds.clear()
            resetLastSwiped()
        }
        notifyItemRangeChanged(0, notes.size, PAYLOAD_SELECT_MODE)
    }

    fun toggleSelection(id: String) {
        if (selectedIds.remove(id).not()) selectedIds.add(id)
    }

    fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(notes.map { it.id })
        notifyItemRangeChanged(0, notes.size, PAYLOAD_SELECT_MODE)
    }

    fun clearSelection() {
        selectedIds.clear()
        notifyItemRangeChanged(0, notes.size, PAYLOAD_SELECT_MODE)
    }

    fun getSelectedIds(): Set<String> = selectedIds.toSet()

    fun getNoteAt(position: Int): Note = notes[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(notes[position])
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        val note = notes[position]
        when {
            payloads.contains(PAYLOAD_SELECT_MODE) -> holder.bindSelectionState(note)
            payloads.contains(PAYLOAD_META) -> holder.bindMeta(note)
            else -> holder.bind(note)
        }
    }

    override fun getItemCount(): Int = notes.size

    private fun animateSwipeBack(content: View?, delete: View?) {
        if (content == null) return
        val density = content.context.resources.displayMetrics.density
        val w = SWIPE_WIDTH_DP * density
        ObjectAnimator.ofFloat(content, "translationX", 0f).apply { duration = 200; start() }
        if (delete != null) {
            ObjectAnimator.ofFloat(delete, "translationX", w).apply { duration = 200; start() }
        }
        // also reset pin button
        content.parent?.let { p ->
            (p as? ViewGroup)?.findViewById<View>(R.id.swipe_pin)?.let {
                ObjectAnimator.ofFloat(it, "translationX", w).apply { duration = 200; start() }
            }
        }
        if (lastSwipedContent == content) {
            lastSwipedContent = null
            lastSwipedDelete = null
        }
    }

    private fun resetLastSwiped() {
        animateSwipeBack(lastSwipedContent, lastSwipedDelete)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val noteContent = view.findViewById<View>(R.id.note_content)
        private val swipeDelete = view.findViewById<View>(R.id.swipe_delete)
        private val swipePin = view.findViewById<View>(R.id.swipe_pin)
        private val tvPinLabel = view.findViewById<TextView>(R.id.tv_pin_label)
        private val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        private val tvPreview = view.findViewById<TextView>(R.id.tv_preview)
        private val tvTime = view.findViewById<TextView>(R.id.tv_time)
        private val checkbox = view.findViewById<View>(R.id.checkbox)

        init {
            // 笔记卡片真玻璃背景
            view.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.note_card_glass)
                ?.setContent {
                    com.mynote.android.ui.glass.GlassCardBackground()
                }
        }
        private var startX = 0f
        private var startY = 0f
        private var isMoving = false
        private var isHorizontalDragging = false
        private var currentNote: Note? = null
        private val density = itemView.context.resources.displayMetrics.density
        private val Float.dp: Float get() = this * density
        private val slop: Float = 8 * density

        fun bind(note: Note) {
            currentNote = note
            val q = searchQuery
            tvTitle.text = if (q.isNotBlank()) highlightText(note.title.ifEmpty { "无标题" }, q) else note.title.ifEmpty { "无标题" }
            tvPreview.text = if (q.isNotBlank()) highlightText(previewText(note.contentText), q) else previewText(note.contentText)
            tvTime.text = note.updateTime.takeLast(11)

            // 置顶图标
            tvTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                if (note.isPinned) R.drawable.ic_pin else 0, 0)

            if (isSelectMode) {
                checkbox.visibility = View.VISIBLE
                val selected = selectedIds.contains(note.id)
                checkbox.isSelected = selected
                checkbox.setBackgroundResource(
                    if (selected) android.R.color.holo_green_light
                    else android.R.color.transparent
                )
            } else {
                checkbox.visibility = View.GONE
                checkbox.isSelected = false
            }

            // 重置滑动状态：两个按钮都隐藏在右侧外
            noteContent.translationX = 0f
            swipeDelete.translationX = SWIPE_WIDTH_DP.dp
            swipePin.translationX = SWIPE_WIDTH_DP.dp

            noteContent.setOnTouchListener { v, event ->
                handleSwipeTouch(v, event)
            }

            noteContent.setOnClickListener {
                if (isSelectMode) {
                    toggleSelection(note.id)
                    notifyItemChanged(adapterPosition)
                } else if (noteContent.translationX != 0f) {
                    animateSwipeBack(noteContent, swipeDelete)
                } else {
                    onItemClick(note)
                }
            }

            swipeDelete.setOnClickListener {
                animateSwipeBack(noteContent, swipeDelete)
                onSwipeDelete(note, adapterPosition)
            }

            tvPinLabel.text = if (note.isPinned) "取消" else "置顶"
            swipePin.setOnClickListener {
                animateSwipeBack(noteContent, swipeDelete)
                onTogglePin(note)
            }
        }

        fun bindSelectionState(note: Note) {
            currentNote = note
            if (isSelectMode) {
                checkbox.visibility = View.VISIBLE
                val selected = selectedIds.contains(note.id)
                checkbox.isSelected = selected
                checkbox.setBackgroundResource(
                    if (selected) android.R.color.holo_green_light
                    else android.R.color.transparent
                )
            } else {
                checkbox.visibility = View.GONE
                checkbox.isSelected = false
            }
        }

        fun bindMeta(note: Note) {
            currentNote = note
            tvTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                if (note.isPinned) R.drawable.ic_pin else 0, 0)
            tvPinLabel.text = if (note.isPinned) "取消" else "置顶"
            tvTime.text = note.updateTime.takeLast(11)
        }

        private fun handleSwipeTouch(view: View, event: MotionEvent): Boolean {
            if (isSelectMode) return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    isMoving = false
                    isHorizontalDragging = false
                    if (lastSwipedContent != null && lastSwipedContent != view) {
                        animateSwipeBack(lastSwipedContent!!, lastSwipedDelete)
                        lastSwipedContent = null
                        lastSwipedDelete = null
                    }
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    val diffX = startX - event.rawX
                    val diffY = startY - event.rawY
                    if (!isMoving) {
                        if (abs(diffX) > slop || abs(diffY) > slop) {
                            isMoving = true
                            isHorizontalDragging = abs(diffX) > abs(diffY)
                        }
                    }
                    if (isHorizontalDragging) {
                        val offset = -diffX.coerceIn(0f, SWIPE_WIDTH_DP.dp)
                        view.translationX = offset
                        swipeDelete.translationX = SWIPE_WIDTH_DP.dp + offset
                        swipePin.translationX = SWIPE_WIDTH_DP.dp + offset
                        return true
                    }
                    return false
                }
                MotionEvent.ACTION_UP -> {
                    if (isHorizontalDragging) {
                        if (view.translationX <= -SWIPE_THRESHOLD_DP.dp) {
                            view.translationX = -SWIPE_WIDTH_DP.dp
                            swipeDelete.translationX = 0f
                            swipePin.translationX = 0f
                            lastSwipedContent = view
                            lastSwipedDelete = swipeDelete
                        } else {
                            animateSwipeBack(view, swipeDelete)
                        }
                        return true
                    }
                    return false
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (isHorizontalDragging) {
                        animateSwipeBack(view, swipeDelete)
                        return true
                    }
                    return false
                }
            }
            return false
        }

        private fun highlightText(text: String, query: String): SpannableString {
            val spannable = SpannableString(text)
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            var start = 0
            while (true) {
                val idx = lowerText.indexOf(lowerQuery, start)
                if (idx < 0) break
                spannable.setSpan(
                    BackgroundColorSpan(Color.parseColor("#FFEB3B")),
                    idx, idx + query.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                start = idx + query.length
            }
            return spannable
        }

        private fun previewText(text: String): String {
            val clean = text.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
            return if (clean.length > 80) clean.take(80) + "..." else clean.ifEmpty { "无内容" }
        }
    }
}
