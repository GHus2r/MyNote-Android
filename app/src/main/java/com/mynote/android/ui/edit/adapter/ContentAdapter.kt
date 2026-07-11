package com.mynote.android.ui.edit.adapter

import android.animation.ObjectAnimator
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.mynote.android.R
import com.mynote.android.data.entity.ContentItem
import com.mynote.android.ui.edit.widget.ShapeView
import java.io.File

/** 记录某个 EditText 后续输入应保持的颜色和起始位置 */
data class ColorSpanInfo(val color: Int, val start: Int)

class ContentAdapter(
    private val onTextChanged: (Int, String) -> Unit,
    private val onImageClick: (Int, String) -> Unit,
    private val onVoicePlayClick: (Int, String) -> Unit,
    private val onVideoClick: (Int, String) -> Unit,
    private val onPdfClick: (Int, String) -> Unit,
    private val onDocClick: (Int, String) -> Unit,
    private val onShapeChanged: (Int, ContentItem) -> Unit,
    private val onShapeLongPress: (Int) -> Unit,
    private val onShapeDoubleTap: (Int) -> Unit,
    private val onItemDelete: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onRequestDelete: ((Int) -> Unit)? = null  // 请求删除确认回调

    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_IMAGE = 1
        const val TYPE_VOICE = 2
        const val TYPE_VIDEO = 3
        const val TYPE_SHAPE = 4
        const val TYPE_PDF = 5
        const val TYPE_DOC = 6
    }

    private val items = mutableListOf<ContentItem>()

    fun submitList(newList: List<ContentItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun getItems(): List<ContentItem> = items.toList()

    fun addItem(item: ContentItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun updateItem(position: Int, item: ContentItem) {
        if (position in items.indices) {
            items[position] = item
            notifyItemChanged(position)
        }
    }

    fun updateItemText(position: Int, text: String) {
        if (position in items.indices) {
            val old = items[position]
            items[position] = old.copy(content = text)
        }
    }

    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].type) {
            "text", "html" -> TYPE_TEXT
            "image" -> TYPE_IMAGE
            "voice" -> TYPE_VOICE
            "video" -> TYPE_VIDEO
            "shape" -> TYPE_SHAPE
            "pdf" -> TYPE_PDF
            "docx", "xlsx", "pptx" -> TYPE_DOC
            else -> TYPE_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TEXT -> TextVH(inflater.inflate(R.layout.item_content_text, parent, false))
            TYPE_IMAGE -> ImageVH(inflater.inflate(R.layout.item_content_image, parent, false))
            TYPE_VOICE -> VoiceVH(inflater.inflate(R.layout.item_content_voice, parent, false))
            TYPE_VIDEO -> VideoVH(inflater.inflate(R.layout.item_content_video, parent, false))
            TYPE_SHAPE -> ShapeVH(inflater.inflate(R.layout.item_content_shape, parent, false))
            TYPE_PDF -> PdfVH(inflater.inflate(R.layout.item_content_pdf, parent, false))
            TYPE_DOC -> DocVH(inflater.inflate(R.layout.item_content_doc, parent, false))
            else -> TextVH(inflater.inflate(R.layout.item_content_text, parent, false))
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is TextVH -> holder.bind(item, position, onTextChanged)
            is ImageVH -> holder.bind(item, position, onImageClick)
            is VoiceVH -> holder.bind(item, position, onVoicePlayClick)
            is VideoVH -> holder.bind(item, position, onVideoClick)
            is ShapeVH -> holder.bind(item, position)
            is PdfVH -> holder.bind(item, position, onPdfClick)
            is DocVH -> holder.bind(item, position, onDocClick)
        }
    }

    // ===== ViewHolders =====
    inner class TextVH(view: View) : RecyclerView.ViewHolder(view) {
        private val etText = view.findViewById<EditText>(R.id.et_text)

        fun bind(item: ContentItem, position: Int, callback: (Int, String) -> Unit) {
            etText.setText(if (item.content.isNotEmpty()) android.text.Html.fromHtml(item.content) else "")
            etText.tag = null // 复用 ViewHolder 时清空运行时颜色状态
            etText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    itemView.tag = position
                }
            }
            etText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    s?.let { applyPendingColor(it) }
                    val html = s?.let { android.text.Html.toHtml(it) } ?: ""
                    callback(position, html)
                }
            })
        }

        private fun applyPendingColor(s: android.text.Editable) {
            val info = etText.tag as? ColorSpanInfo ?: return
            val start = info.start.coerceIn(0, s.length)
            s.setSpan(
                ForegroundColorSpan(info.color),
                start,
                s.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }
    }

    inner class ImageVH(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ImageView>(R.id.iv_image)

        fun bind(item: ContentItem, position: Int, callback: (Int, String) -> Unit) {
            val file = File(item.content)
            if (file.exists()) {
                imageView.setImageURI(android.net.Uri.fromFile(file))
            } else {
                imageView.setImageResource(R.drawable.ic_image)
            }
            imageView.setOnClickListener { callback(position, item.content) }
        }
    }

    inner class VoiceVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDuration = view.findViewById<TextView>(R.id.tv_voice_duration)
        private val btnPlay = view.findViewById<ImageButton>(R.id.btn_voice_play)
        private val tvName = view.findViewById<TextView>(R.id.tv_voice_name)

        fun bind(item: ContentItem, position: Int, callback: (Int, String) -> Unit) {
            tvName.text = File(item.content).name
            tvDuration.text = formatDuration(item.voiceDuration)
            btnPlay.setOnClickListener { callback(position, item.content) }
        }

        private fun formatDuration(ms: Long): String {
            if (ms <= 0) return "00:00"
            val sec = ms / 1000
            return String.format("%02d:%02d", sec / 60, sec % 60)
        }
    }

    inner class VideoVH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivThumb = view.findViewById<ImageView>(R.id.iv_video_thumb)
        private val btnPlay = view.findViewById<ImageView>(R.id.btn_video_play)

        fun bind(item: ContentItem, position: Int, callback: (Int, String) -> Unit) {
            val file = File(item.content)
            if (file.exists()) {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    val bmp = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ivThumb.setImageBitmap(bmp)
                } catch (e: Exception) {
                    ivThumb.setImageResource(R.drawable.ic_play)
                } finally {
                    retriever.release()
                }
            } else {
                ivThumb.setImageResource(R.drawable.ic_play)
            }
            itemView.setOnClickListener { callback(position, item.content) }
        }
    }

    inner class ShapeVH(view: View) : RecyclerView.ViewHolder(view) {
        private val shapeView = view.findViewById<ShapeView>(R.id.shape_view)

        fun bind(item: ContentItem, position: Int) {
            shapeView.fromContent(item.content)
            shapeView.onShapeChanged = {
                items[position] = items[position].copy(content = shapeView.toContent())
                onShapeChanged(position, items[position])
            }
            shapeView.onLongPress = { onShapeLongPress(position) }
            shapeView.onDoubleTap = { onShapeDoubleTap(position) }
        }

        private fun shapeName(type: String): String = when (type.split("|")[0]) {
            "circle" -> "圆形"
            "square" -> "方形"
            "triangle" -> "三角形"
            "diamond" -> "菱形"
            "pentagon" -> "五边形"
            "hexagon" -> "六边形"
            "star" -> "星形"
            "heart" -> "心形"
            else -> type
        }
    }

    inner class PdfVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName = view.findViewById<TextView>(R.id.tv_pdf_name)
        private val ivIcon = view.findViewById<ImageView>(R.id.iv_pdf_icon)

        fun bind(item: ContentItem, position: Int, callback: (Int, String) -> Unit) {
            tvName.text = File(item.content).name
            ivIcon.setImageResource(R.drawable.ic_image)
            itemView.setOnClickListener { callback(position, item.content) }
        }
    }

    inner class DocVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName = view.findViewById<TextView>(R.id.tv_doc_name)
        private val tvType = view.findViewById<TextView>(R.id.tv_doc_type)
        private val ivIcon = view.findViewById<TextView>(R.id.iv_doc_icon)

        fun bind(item: ContentItem, position: Int, callback: (Int, String) -> Unit) {
            tvName.text = File(item.content).name
            tvType.text = when (item.type) {
                "docx" -> "Word 文档"
                "xlsx" -> "Excel 表格"
                "pptx" -> "PPT 演示文稿"
                else -> item.type.uppercase()
            }
            ivIcon.text = when (item.type) {
                "docx" -> "\uD83D\uDCC4"
                "xlsx" -> "\uD83D\uDCCA"
                "pptx" -> "\uD83D\uDCC8"
                else -> "\uD83D\uDCC1"
            }
            itemView.setOnClickListener { callback(position, item.content) }
        }
    }

    fun getItemTouchCallback(): ItemTouchHelper.SimpleCallback {
        return object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // 文本项和形状项不显示左滑删除
                val type = viewHolder.itemViewType
                return if (type == TYPE_TEXT || type == TYPE_SHAPE) 0 else super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onRequestDelete?.invoke(pos) ?: onItemDelete(pos)
                }
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                // 红色删除背景
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val bg = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#F44336"))
                    bg.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    bg.draw(c)
                    // "删除" 文字
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE; textSize = 36f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val cx = itemView.right - 72f
                    val cy = itemView.top + itemView.height / 2f + 12f
                    c.drawText("删除", cx, cy, paint)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
    }
}
