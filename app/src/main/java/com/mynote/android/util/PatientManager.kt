package com.mynote.android.util

import android.content.Context
import com.google.gson.Gson

/**
 * 患者管理 + 随访提醒
 * SharedPreferences JSON 存储
 */
object PatientManager {
    data class Patient(
        val id: String = java.util.UUID.randomUUID().toString().take(8),
        val name: String,
        val age: String,
        val gender: String,
        val diagnosis: String,
        val phone: String,
        val followUpDate: String = "",
        val notes: String = ""
    )

    private var ctx: Context? = null
    fun init(context: Context) { ctx = context.applicationContext }
    private fun sp() = requireNotNull(ctx) { "PatientManager 未初始化，请先调用 init()" }
        .getSharedPreferences("MyNotePrefs", Context.MODE_PRIVATE)

    private val gson = Gson()
    private val type = com.google.gson.reflect.TypeToken.getParameterized(MutableList::class.java, Patient::class.java).type

    fun getAll(): MutableList<Patient> {
        val j = sp().getString("patients", "[]") ?: "[]"
        return try { gson.fromJson(j, type) ?: mutableListOf() } catch(_: Exception) { mutableListOf() }
    }

    private fun save(list: MutableList<Patient>) {
        sp().edit().putString("patients", gson.toJson(list)).apply()
    }

    fun add(p: Patient) { val l = getAll(); l.add(p); save(l) }
    fun update(p: Patient) { val l = getAll(); val i = l.indexOfFirst { it.id == p.id }; if (i >= 0) l[i] = p; save(l) }
    fun delete(id: String) { val l = getAll(); l.removeAll { it.id == id }; save(l) }

    fun todayFollowUps(): List<Patient> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return getAll().filter { it.followUpDate == today }
    }

    fun upcomingFollowUps(days: Int = 7): List<Patient> {
        val cal = java.util.Calendar.getInstance(); val today = cal.time
        cal.add(java.util.Calendar.DAY_OF_MONTH, days); val end = cal.time
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return getAll().filter { p ->
            if (p.followUpDate.isBlank()) return@filter false
            try { val d = fmt.parse(p.followUpDate); d != null && !d.before(today) && !d.after(end) } catch(_: Exception) { false }
        }
    }
}
