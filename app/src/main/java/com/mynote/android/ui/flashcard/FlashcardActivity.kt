package com.mynote.android.ui.flashcard

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mynote.android.R
import java.util.UUID

class FlashcardActivity : AppCompatActivity() {

    private var cards = listOf<FlashCard>()
    private var currentIdx = 0
    private var showingFront = true

    private lateinit var tvFront: TextView
    private lateinit var tvBack: TextView
    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard)

        tvFront = findViewById(R.id.fc_front)
        tvBack = findViewById(R.id.fc_back)
        tvProgress = findViewById(R.id.fc_progress)
        progressBar = findViewById(R.id.fc_progress_bar)

        cards = FlashCardStore.dueCards(this)
        if (cards.isEmpty()) {
            Toast.makeText(this, "暂无需要复习的卡片，请先添加", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.fc_back_btn).setOnClickListener { finish() }
        findViewById<TextView>(R.id.fc_add).setOnClickListener { showAddDialog() }
        findViewById<TextView>(R.id.fc_all).setOnClickListener { showAllCards() }

        tvFront.setOnClickListener { flipCard() }
        tvBack.setOnClickListener { flipCard() }

        findViewById<TextView>(R.id.fc_again).setOnClickListener { rateCard(0) }
        findViewById<TextView>(R.id.fc_hard).setOnClickListener { rateCard(2) }
        findViewById<TextView>(R.id.fc_good).setOnClickListener { rateCard(4) }
        findViewById<TextView>(R.id.fc_easy).setOnClickListener { rateCard(5) }

        showCurrentCard()
    }

    private fun showCurrentCard() {
        if (cards.isEmpty() || currentIdx >= cards.size) {
            Toast.makeText(this, "🎉 本日复习完成！", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val card = cards[currentIdx]
        tvFront.text = card.front
        tvBack.text = card.back
        tvBack.visibility = View.GONE
        tvFront.visibility = View.VISIBLE
        showingFront = true
        tvProgress.text = "${currentIdx + 1} / ${cards.size}"
        progressBar.max = cards.size
        progressBar.progress = currentIdx + 1
    }

    private fun flipCard() {
        if (showingFront) {
            tvFront.visibility = View.GONE
            tvBack.visibility = View.VISIBLE
        } else {
            tvBack.visibility = View.GONE
            tvFront.visibility = View.VISIBLE
        }
        showingFront = !showingFront
    }

    private fun rateCard(quality: Int) {
        if (currentIdx >= cards.size) return
        val card = cards[currentIdx]
        val (ef, interval, reps) = SpacedRepetition.nextInterval(card.easiness, card.interval, card.repetitions, quality)
        val updated = card.copy(
            easiness = ef, interval = interval, repetitions = reps,
            nextReview = System.currentTimeMillis() + interval * 24 * 60 * 60 * 1000L
        )
        cards = cards.toMutableList().apply { this[currentIdx] = updated }
        FlashCardStore.save(this, cards)
        currentIdx++
        showCurrentCard()
    }

    private fun showAddDialog() {
        val frontInput = EditText(this).apply {
            hint = "正面（问题 / 术语）"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val backInput = EditText(this).apply {
            hint = "背面（答案 / 解释）"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(frontInput, android.widget.LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })
            addView(backInput, android.widget.LinearLayout.LayoutParams(-1, -2))
        }
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("添加闪卡")
            .setView(layout)
            .setPositiveButton("添加") { _, _ ->
                val front = frontInput.text.toString().trim()
                val back = backInput.text.toString().trim()
                if (front.isEmpty() || back.isEmpty()) {
                    Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val all = FlashCardStore.load(this).toMutableList()
                all.add(FlashCard(id = UUID.randomUUID().toString(), front = front, back = back))
                FlashCardStore.save(this, all)
                cards = all.filter { it.nextReview <= System.currentTimeMillis() }
                Toast.makeText(this, "已添加 (共${all.size}张)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAllCards() {
        val all = FlashCardStore.load(this)
        if (all.isEmpty()) {
            Toast.makeText(this, "暂无卡片", Toast.LENGTH_SHORT).show()
            return
        }
        val items = all.mapIndexed { i, c -> "${i + 1}. ${c.front}" }.toTypedArray()
        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("全部卡片 (${all.size})")
            .setItems(items, null)
            .setPositiveButton("删除全部") { _, _ ->
                FlashCardStore.save(this, emptyList())
                Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    override fun onBackPressed() {
        FlashCardStore.save(this, cards)
        super.onBackPressed()
    }
}
