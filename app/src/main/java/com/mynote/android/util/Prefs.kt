package com.mynote.android.util

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences 封装
 * 对应 uni-app 的 uni.setStorageSync / getStorageSync
 * 存储:密码设置、防卸载设置、百度备份 token、锁屏状态等非结构化数据
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ===== 密码设置 =====
    var passwordEnabled: Boolean
        get() = sp.getBoolean(KEY_PASSWORD_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_PASSWORD_ENABLED, value).apply()

    var fingerprintEnabled: Boolean
        get() = sp.getBoolean(KEY_FINGERPRINT_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_FINGERPRINT_ENABLED, value).apply()

    var password: String?
        get() = sp.getString(KEY_PASSWORD, null)
        set(value) = sp.edit().putString(KEY_PASSWORD, value).apply()

    /** 自动锁定时间(分钟),-1 表示从不,0 表示立即 */
    var lockTimeMinutes: Int
        get() = sp.getInt(KEY_LOCK_TIME, 0)
        set(value) = sp.edit().putInt(KEY_LOCK_TIME, value).apply()

    // ===== 锁屏状态 =====
    var lastUnlockTime: Long
        get() = sp.getLong(KEY_LAST_UNLOCK, 0)
        set(value) = sp.edit().putLong(KEY_LAST_UNLOCK, value).apply()

    var justUnlocked: Boolean
        get() = sp.getBoolean(KEY_JUST_UNLOCKED, false)
        set(value) = sp.edit().putBoolean(KEY_JUST_UNLOCKED, value).apply()

    var appOnHideTime: Long
        get() = sp.getLong(KEY_APP_ON_HIDE, 0)
        set(value) = sp.edit().putLong(KEY_APP_ON_HIDE, value).apply()

    // ===== 防卸载 =====
    var antiUninstallEnabled: Boolean
        get() = sp.getBoolean(KEY_ANTI_UNINSTALL, false)
        set(value) = sp.edit().putBoolean(KEY_ANTI_UNINSTALL, value).apply()

    // ===== 摇一摇快速录音 =====
    var quickRecordEnabled: Boolean
        get() = sp.getBoolean(KEY_QUICK_RECORD, false)
        set(value) = sp.edit().putBoolean(KEY_QUICK_RECORD, value).apply()

    /** 知识库版本日期 */
    var kbVersion: String
        get() = sp.getString("kb_version", "2026.07") ?: "2026.07"
        set(value) = sp.edit().putString("kb_version", value).apply()

    /** 计算器历史 */
    fun calcHistoryList(): MutableList<String> {
        val raw = sp.getString("calc_history", "") ?: ""
        return raw.split("\n--SEP--\n").filter { it.isNotBlank() }.toMutableList()
    }

    fun addCalcHistory(entry: String) {
        val list = calcHistoryList()
        list.add(0, entry)
        if (list.size > 50) { while (list.size > 50) list.removeAt(list.lastIndex) }
        sp.edit().putString("calc_history", list.joinToString("\n--SEP--\n")).apply()
    }

    fun clearCalcHistory() {
        sp.edit().putString("calc_history", "").apply()
    }

    /** 疾病收藏 (StringSet) */
    fun getStringSet(key: String, def: Set<String>): Set<String> =
        sp.getStringSet(key, def) ?: def

    fun putStringSet(key: String, value: Set<String>) {
        sp.edit().putStringSet(key, value).apply()
    }

    fun getString(key: String, def: String): String =
        sp.getString(key, def) ?: def

    fun putString(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }

    /** 摇一摇灵敏度：true=灵敏(3次)，false=迟钝(4次) */
    var shakeSensitive: Boolean
        get() = sp.getBoolean(KEY_SHAKE_SENSITIVE, true)
        set(value) = sp.edit().putBoolean(KEY_SHAKE_SENSITIVE, value).apply()

    // ===== 讯飞语音识别 =====
    var iatAppId: String
        get() = sp.getString(KEY_IAT_APP_ID, "") ?: ""
        set(value) = sp.edit().putString(KEY_IAT_APP_ID, value).apply()

    var iatApiKey: String
        get() = sp.getString(KEY_IAT_API_KEY, "") ?: ""
        set(value) = sp.edit().putString(KEY_IAT_API_KEY, value).apply()

    var iatApiSecret: String
        get() = sp.getString(KEY_IAT_API_SECRET, "") ?: ""
        set(value) = sp.edit().putString(KEY_IAT_API_SECRET, value).apply()

    // ===== 天翼AI 维吾尔语识别 =====
    var tianyiAppId: String
        get() = sp.getString(KEY_TIANYI_APP_ID, "") ?: ""
        set(value) = sp.edit().putString(KEY_TIANYI_APP_ID, value).apply()

    var tianyiApiKey: String
        get() = sp.getString(KEY_TIANYI_API_KEY, "") ?: ""
        set(value) = sp.edit().putString(KEY_TIANYI_API_KEY, value).apply()

    val tianyiDeviceUuid: String
        get() {
            var uuid = sp.getString(KEY_TIANYI_DEVICE_UUID, null)
            if (uuid == null) {
                uuid = java.util.UUID.randomUUID().toString()
                sp.edit().putString(KEY_TIANYI_DEVICE_UUID, uuid).apply()
            }
            return uuid
        }

    // ===== 阿里云 Qwen 维语识别 =====
    var qwenApiKey: String
        get() = sp.getString(KEY_QWEN_API_KEY, "") ?: ""
        set(value) = sp.edit().putString(KEY_QWEN_API_KEY, value).apply()

    var qwenApiSecret: String
        get() = sp.getString(KEY_QWEN_API_SECRET, "") ?: ""
        set(value) = sp.edit().putString(KEY_QWEN_API_SECRET, value).apply()

    // ===== DeepSeek AI 病历生成 =====
    var deepseekApiKey: String
        get() = sp.getString(KEY_DEEPSEEK_API_KEY, "") ?: ""
        set(value) = sp.edit().putString(KEY_DEEPSEEK_API_KEY, value).apply()

    // ===== 百度网盘备份 =====
    var baiduAccessToken: String?
        get() = sp.getString(KEY_BAIDU_ACCESS_TOKEN, null)
        set(value) = sp.edit().putString(KEY_BAIDU_ACCESS_TOKEN, value).apply()

    var baiduRefreshToken: String?
        get() = sp.getString(KEY_BAIDU_REFRESH_TOKEN, null)
        set(value) = sp.edit().putString(KEY_BAIDU_REFRESH_TOKEN, value).apply()

    var baiduTokenExpiresTime: Long
        get() = sp.getLong(KEY_BAIDU_EXPIRES, 0)
        set(value) = sp.edit().putLong(KEY_BAIDU_EXPIRES, value).apply()

    var baiduAutoBackup: Boolean
        get() = sp.getBoolean(KEY_BAIDU_AUTO, false)
        set(value) = sp.edit().putBoolean(KEY_BAIDU_AUTO, value).apply()

    var lastBackupTime: String?
        get() = sp.getString(KEY_LAST_BACKUP_TIME, null)
        set(value) = sp.edit().putString(KEY_LAST_BACKUP_TIME, value).apply()

    var cosSecretId: String
        get() = sp.getString(KEY_COS_SECRET_ID, "") ?: ""
        set(value) = sp.edit().putString(KEY_COS_SECRET_ID, value).apply()

    var cosSecretKey: String
        get() = sp.getString(KEY_COS_SECRET_KEY, "") ?: ""
        set(value) = sp.edit().putString(KEY_COS_SECRET_KEY, value).apply()

    // ===== 外观 =====
    @Deprecated("暗色模式已移除", ReplaceWith("false"))
    var darkMode: Boolean
        get() = false
        set(_) = Unit

    /** 用户已点过"暂不"创建快捷方式，之后不再弹窗 */
    var shortcutPromptDismissed: Boolean
        get() = sp.getBoolean(KEY_SHORTCUT_PROMPT_DISMISSED, false)
        set(value) = sp.edit().putBoolean(KEY_SHORTCUT_PROMPT_DISMISSED, value).apply()

    /** 用户上次通过快捷方式进入，再次打开时跳过伪装计算器 */
    var wasShortcutSession: Boolean
        get() = sp.getBoolean(KEY_WAS_SHORTCUT, false)
        set(value) = sp.edit().putBoolean(KEY_WAS_SHORTCUT, value).apply()

    /** 活跃会话时间戳：解锁成功时写入，LockActivity 重建时判断会话是否仍有效 */
    var appSessionTimestamp: Long
        get() = sp.getLong(KEY_SESSION_TS, 0)
        set(value) = sp.edit().putLong(KEY_SESSION_TS, value).apply()

    /** 已创建过科室预设 */
    var departmentsSeeded: Boolean
        get() = sp.getBoolean(KEY_DEPT_SEEDED, false)
        set(value) = sp.edit().putBoolean(KEY_DEPT_SEEDED, value).apply()

    /** v1.0.2 数据库重建后重置标记 */
    var departmentsSeededV4: Boolean
        get() = sp.getBoolean("departments_seeded_v4", false)
        set(value) = sp.edit().putBoolean("departments_seeded_v4", value).apply()

    var emojiCleaned: Boolean
        get() = sp.getBoolean("emoji_cleaned", false)
        set(value) = sp.edit().putBoolean("emoji_cleaned", value).apply()

    // 搜索历史（最多10条）
    var searchHistory: List<String>
        get() {
            val json = sp.getString("search_history", "[]") ?: "[]"
            return try {
                com.google.gson.Gson().fromJson(json, Array<String>::class.java).toList()
            } catch (_: Exception) { emptyList() }
        }
        set(value) = sp.edit().putString("search_history",
            com.google.gson.Gson().toJson(value.take(10))).apply()

    fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val history = searchHistory.toMutableList()
        history.remove(trimmed)
        history.add(0, trimmed)
        searchHistory = history
    }

    fun clearSearchHistory() {
        sp.edit().remove("search_history").apply()
    }

    companion object {
        private const val PREF_NAME = "mynote_prefs"

        private const val KEY_PASSWORD_ENABLED = "password_enabled"
        private const val KEY_FINGERPRINT_ENABLED = "fingerprint_enabled"
        private const val KEY_PASSWORD = "password"
        private const val KEY_LOCK_TIME = "lock_time_minutes"

        private const val KEY_LAST_UNLOCK = "last_unlock_time"
        private const val KEY_JUST_UNLOCKED = "just_unlocked"
        private const val KEY_APP_ON_HIDE = "app_on_hide_time"

        private const val KEY_ANTI_UNINSTALL = "anti_uninstall_enabled"
        private const val KEY_QUICK_RECORD = "quick_record_enabled"
        private const val KEY_SHAKE_SENSITIVE = "shake_sensitive"
        private const val KEY_IAT_APP_ID = "iat_app_id"
        private const val KEY_IAT_API_KEY = "iat_api_key"
        private const val KEY_IAT_API_SECRET = "iat_api_secret"
        private const val KEY_TIANYI_APP_ID = "tianyi_app_id"
        private const val KEY_TIANYI_API_KEY = "tianyi_api_key"
        private const val KEY_TIANYI_DEVICE_UUID = "tianyi_device_uuid"
        private const val KEY_QWEN_API_KEY = "qwen_api_key"
        private const val KEY_QWEN_API_SECRET = "qwen_api_secret"
        private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"

        private const val KEY_BAIDU_ACCESS_TOKEN = "baidu_access_token"
        private const val KEY_BAIDU_REFRESH_TOKEN = "baidu_refresh_token"
        private const val KEY_BAIDU_EXPIRES = "baidu_expires_time"
        private const val KEY_BAIDU_AUTO = "baidu_auto_backup"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_COS_SECRET_ID = "baidu_app_key"
        private const val KEY_COS_SECRET_KEY = "baidu_secret_key"
        private const val KEY_AUTO_SYNC = "auto_sync_to_pc"

        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SHORTCUT_PROMPT_DISMISSED = "shortcut_prompt_dismissed"
        private const val KEY_WAS_SHORTCUT = "was_shortcut_session"
        private const val KEY_SESSION_TS = "app_session_timestamp"
        private const val KEY_DEPT_SEEDED = "departments_seeded"
    }
}
