package com.mynote.android

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import com.mynote.android.data.AppDatabase
import com.mynote.android.data.entity.ParentCategory
import com.mynote.android.data.entity.SubCategory
import com.mynote.android.ui.settings.AdminReceiver
import com.mynote.android.ui.recording.QuickRecordService
import com.mynote.android.util.CrashHandler
import com.mynote.android.util.DeptTemplates
import com.mynote.android.util.DiseaseReference
import com.mynote.android.util.DrugReference
import com.mynote.android.util.IconManager
import com.mynote.android.util.PatientManager
import com.mynote.android.util.Prefs
import com.mynote.android.util.TemplateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class MyNoteApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 崩溃日志收集
        CrashHandler.init(this)

        // 患者管理初始化
        PatientManager.init(this)

        // 知识库离线增量更新检查
        com.mynote.android.util.DataUpdater.checkUpdate(this)

        // 疾病数据从 assets JSON 预加载
        DiseaseReference.init(this)
        DrugReference.init(this)
        TemplateManager.init(this)
        DeptTemplates.init(this)

        // 方案C：图标始终显示（伪装模式），不需要显隐控制

        // 检查设备管理员状态：曾经激活但现在被撤销 → 强制锁屏
        checkAdminStatus()

        // 首次启动 → 创建科室预设
        seedDepartments()

        // 清除分类名称中的 emoji（只执行一次）
        cleanCategoryEmojis()

        // 清理过期回收站（30天）
        cleanExpiredTrash()

        // 自动启动摇一摇录音服务（如果已开启）
        if (Prefs(this).quickRecordEnabled) {
            try { QuickRecordService.start(this) } catch (_: Exception) {}
        }
    }

    /**
     * 启动时检测防卸载保护状态。
     */
    private fun checkAdminStatus() {
        val prefs = Prefs(this)
        if (!prefs.antiUninstallEnabled) return

        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(this, AdminReceiver::class.java)
        val isActive = dpm.isAdminActive(component)

        if (!isActive && prefs.antiUninstallEnabled) {
            prefs.antiUninstallEnabled = false
            prefs.passwordEnabled = true
            prefs.justUnlocked = false
        }
    }

    /**
     * 首次启动时创建规培科室分类
     */
    private fun seedDepartments() {
        val prefs = Prefs(this)
        // DB 重建后标记失效，强制重新检查
        if (!prefs.departmentsSeededV4) {
            prefs.departmentsSeeded = false
            prefs.departmentsSeededV4 = true
        }
        appScope.launch {
            try {
                val db = AppDatabase.get(this@MyNoteApp)
                val dao = db.categoryDao()
                val existing = dao.getParentCategories()
                // 已有分类就跳过
                if (existing.isNotEmpty()) {
                    prefs.departmentsSeeded = true
                    return@launch
                }
                val depts = listOf(
                    "内" to listOf("呼吸科", "心血管科", "消化科", "内分泌科", "肾内科", "血液科", "风湿免疫"),
                    "外" to listOf("普外科", "骨科", "泌尿外科", "神经外科", "心胸外科", "整形外科"),
                    "妇" to listOf("产科", "妇科", "生殖医学"),
                    "儿" to listOf("小儿内科", "小儿外科", "新生儿科"),
                    "急" to listOf("急诊", "ICU"),
                    "神内" to listOf("脑血管", "癫痫与发作性疾病", "神经免疫"),
                    "皮" to listOf("常见皮肤病", "性病"),
                    "眼" to listOf("眼前节", "眼底病"),
                    "五官" to listOf("耳科", "鼻科", "咽喉科"),
                    "影像" to listOf("X线", "CT", "MRI", "超声"),
                    "麻醉" to listOf("全身麻醉", "局部麻醉"),
                    "病理" to listOf("组织病理", "细胞病理"),
                    "考试" to listOf("执业医考点", "出科考试", "年度考核", "结业考核"),
                )
                val deptColors = arrayOf("#2196F3", "#4CAF50", "#FF9800", "#F44336", "#9C27B0", "#00BCD4", "#FF5722", "#607D8B", "#795548", "#E91E63", "#3F51B5", "#009688", "#CDDC39")
                for ((idx, dept) in depts.withIndex()) {
                    val (parentName, subNames) = dept
                    val pId = UUID.randomUUID().toString()
                    dao.insertParentCategory(ParentCategory(id = pId, name = parentName, color = deptColors[idx % deptColors.size]))
                    for ((i, subName) in subNames.withIndex()) {
                        dao.insertSubCategory(SubCategory(id = UUID.randomUUID().toString(), parentId = pId, name = subName, sortOrder = i))
                    }
                }
                prefs.departmentsSeeded = true
            } catch (_: Exception) {}
        }
    }

    /** 一次性清除所有父/子分类名称中的 emoji */
    private fun cleanCategoryEmojis() {
        val prefs = Prefs(this)
        if (prefs.emojiCleaned) return
        appScope.launch {
            try {
                val db = AppDatabase.get(this@MyNoteApp)
                val dao = db.categoryDao()
                for (p in dao.getParentCategories()) {
                    val cleaned = stripEmojiFn(p.name)
                    if (cleaned != p.name) {
                        dao.updateParentCategory(p.copy(name = cleaned))
                    }
                }
                for (s in dao.getAllSubCategories()) {
                    val cleaned = stripEmojiFn(s.name)
                    if (cleaned != s.name) {
                        dao.updateSubCategory(s.copy(name = cleaned))
                    }
                }
                prefs.emojiCleaned = true
            } catch (_: Exception) {}
        }
    }

    /**
     * 清理过期回收站（30天）
     */
    private fun cleanExpiredTrash() {
        try {
            appScope.launch {
                val threshold = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                val expiredNotes = database.noteDao().getTrashedNotes().filter { it.trashedAt < threshold && it.trashedAt > 0 }
                expiredNotes.forEach { note ->
                    database.noteDao().deleteNoteById(note.id)
                    try { java.io.File(filesDir, "notes/${note.id}").deleteRecursively() } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    companion object {
        lateinit var instance: MyNoteApp
            private set

        private fun stripEmojiFn(name: String): String {
            val sb = StringBuilder()
            name.codePoints().forEach { cp ->
                if (cp !in 0x1F000..0x1FFFF && cp !in 0x2600..0x27BF &&
                    cp !in 0x2300..0x23FF && cp !in 0x2B50..0x2B55 &&
                    cp !in 0x2702..0x27B0 && cp != 0xFE0F && cp != 0x200D &&
                    cp != 0x20E3 && cp != 0xFE0E &&
                    !(cp in 0xD800..0xDFFF)
                ) {
                    sb.append(Character.toChars(cp))
                }
            }
            return sb.toString().trim()
        }
    }
}
