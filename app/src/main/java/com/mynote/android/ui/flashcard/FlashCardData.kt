package com.mynote.android.ui.flashcard

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.roundToInt

/**
 * SM-2 间隔重复算法
 *
 * q: 用户评分 0-5（0=完全忘记, 3=艰难想起, 5=完美）
 * 返回下一次复习间隔（天）
 */
object SpacedRepetition {

    fun nextInterval(easiness: Double, interval: Int, repetitions: Int, quality: Int): Triple<Double, Int, Int> {
        var ef = easiness
        val q = quality.coerceIn(0, 5)

        ef = ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        ef = ef.coerceAtLeast(1.3)

        val (newReps, newInterval) = if (q < 3) {
            0 to 1
        } else {
            val rep = repetitions + 1
            when (rep) {
                1 -> rep to 1
                2 -> rep to 6
                else -> rep to (interval * ef).roundToInt().coerceAtLeast(1)
            }
        }
        return Triple(ef, newInterval, newReps)
    }
}

/**
 * 闪卡数据模型
 */
data class FlashCard(
    val id: String,
    val front: String,      // 正面（问题/术语）
    val back: String,       // 背面（答案/解释）
    val easiness: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val nextReview: Long = 0,  // 下次复习时间戳
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 闪卡存储（SharedPreferences JSON）
 */
object FlashCardStore {
    private const val KEY = "flashcards_data"

    fun load(ctx: Context): List<FlashCard> {
        val json = ctx.getSharedPreferences("mynote_prefs", Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            com.google.gson.Gson().fromJson(json, Array<FlashCard>::class.java).toList()
        } catch (_: Exception) { emptyList() }
    }

    fun save(ctx: Context, cards: List<FlashCard>) {
        val json = com.google.gson.Gson().toJson(cards)
        ctx.getSharedPreferences("mynote_prefs", Context.MODE_PRIVATE).edit().putString(KEY, json).apply()
    }

    /** 获取今天需要复习的卡片 */
    fun dueCards(ctx: Context): List<FlashCard> {
        val now = System.currentTimeMillis()
        return load(ctx).filter { it.nextReview <= now }
    }
}
