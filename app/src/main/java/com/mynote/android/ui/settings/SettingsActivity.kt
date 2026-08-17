package com.mynote.android.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.biometric.BiometricManager
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.mynote.android.R
import com.mynote.android.data.AppDatabase
import com.mynote.android.ui.base.BaseActivity
import com.mynote.android.ui.lock.LockActivity
import com.mynote.android.util.BackupManager
import com.mynote.android.util.WebDAVBackup
import com.mynote.android.util.CrashHandler
import com.mynote.android.util.IconManager
import com.mynote.android.util.MedicalCalculator
import com.mynote.android.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.webkit.WebView
import android.webkit.WebViewClient
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.android.gms.tasks.Tasks
import okhttp3.MediaType.Companion.toMediaType

class SettingsActivity : BaseActivity() {

    private val db by lazy { AppDatabase.get(application) }
    private val prefs by lazy { Prefs(this) }

    override fun exemptFromLock() = true

    // 安全设置
    private lateinit var switchPassword: SwitchCompat
    private lateinit var itemChangePwd: View
    private lateinit var switchFingerprint: SwitchCompat
    private lateinit var itemFingerprint: View
    private lateinit var tvFingerprintHint: TextView
    private lateinit var itemLockTime: View
    private lateinit var tvLockTimeValue: TextView
    private lateinit var switchAntiUninstall: SwitchCompat
    private lateinit var tvAntiUninstallHint: TextView

    // 备份
    private lateinit var tvLastBackup: TextView
    private lateinit var itemLocalBackup: View
    private lateinit var itemDbBackup: View
    private lateinit var itemExportAll: View
    private lateinit var itemRestoreZip: View
    private lateinit var itemRestoreBackup: View
    private lateinit var itemManageBackups: View
    private lateinit var tvBackupCount: TextView
    private lateinit var itemCreateShortcut: View
    private lateinit var switchQuickRecord: SwitchCompat
    private lateinit var tvQuickRecordHint: TextView
    private lateinit var itemShakeSensitive: View
    private lateinit var tvShakeSensitive: TextView
    private lateinit var itemCrashLogs: View
    private lateinit var itemIatConfig: View
    private lateinit var itemTianyiConfig: View
    private lateinit var itemQwenConfig: View
    private lateinit var itemDeepseekConfig: View
    private lateinit var tvCrashCount: TextView
    private lateinit var itemPatientMgr: View
    private lateinit var tvPatientCount: TextView
    private lateinit var itemStorage: View
    private lateinit var tvStorageSize: TextView
    private var pendingRestoreUri: Uri? = null

    private val lockTimeLabels = listOf("立即", "1分钟", "3分钟", "5分钟", "10分钟", "从不")
    private val lockTimeValues = listOf(0, 1, 3, 5, 10, -1)

    // 防卸载
    private var adminComponent: ComponentName? = null
    private var dpm: DevicePolicyManager? = null
    // 已删除 antiUninstallJustActivated，改用 prefs.shortcutPromptDismissed 持久化标记

    // ---- 文件选择器 ----
    private val jsonExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { exportJsonBackup(it) } }

    private val dbExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { exportDbBackup(it) } }

    private val restorePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onRestoreFilePicked(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        switchPassword = findViewById(R.id.switch_password)
        itemChangePwd = findViewById(R.id.item_change_password)
        switchFingerprint = findViewById(R.id.switch_fingerprint)
        itemFingerprint = findViewById(R.id.item_fingerprint)
        tvFingerprintHint = findViewById(R.id.tv_fingerprint_hint)
        itemLockTime = findViewById(R.id.item_lock_time)
        tvLockTimeValue = findViewById(R.id.tv_lock_time_value)
        switchAntiUninstall = findViewById(R.id.switch_anti_uninstall)
        tvAntiUninstallHint = findViewById(R.id.tv_anti_uninstall_hint)
        tvLastBackup = findViewById(R.id.tv_last_backup)
        itemLocalBackup = findViewById(R.id.item_local_backup)
        itemDbBackup = findViewById(R.id.item_db_backup)
        itemExportAll = findViewById(R.id.item_export_all)
        itemRestoreZip = findViewById(R.id.item_restore_zip)
        itemRestoreBackup = findViewById(R.id.item_restore_backup)
        itemManageBackups = findViewById(R.id.item_manage_backups)
        tvBackupCount = findViewById(R.id.tv_backup_count)
        itemCreateShortcut = findViewById(R.id.item_create_shortcut)
        switchQuickRecord = findViewById(R.id.switch_quick_record)
        tvQuickRecordHint = findViewById(R.id.tv_quick_record_hint)
        itemShakeSensitive = findViewById(R.id.item_shake_sensitive)
        tvShakeSensitive = findViewById(R.id.tv_shake_sensitive)
        itemCrashLogs = findViewById(R.id.item_crash_logs)
        itemIatConfig = findViewById(R.id.item_iat_config)
        itemTianyiConfig = findViewById(R.id.item_tianyi_config)
        itemQwenConfig = findViewById(R.id.item_qwen_config)
        itemDeepseekConfig = findViewById(R.id.item_deepseek_config)
        tvCrashCount = findViewById(R.id.tv_crash_count)
        itemPatientMgr = findViewById(R.id.item_patient_mgr)
        tvPatientCount = findViewById(R.id.tv_patient_count)
        itemStorage = findViewById(R.id.item_storage)
        tvStorageSize = findViewById(R.id.tv_storage_size)

        refreshUI()
        setupListeners()
        initAntiUninstall()

        // 初始化 DDInter 药物相互作用引擎
        com.mynote.android.util.DrugInteractionEngine.init(this)

        findViewById<TextView>(R.id.btn_settings_back).setOnClickListener { finish() }
    }

    private fun refreshUI() {
        switchPassword.isChecked = prefs.passwordEnabled
        itemChangePwd.visibility = if (prefs.passwordEnabled) View.VISIBLE else View.GONE

        val canBio = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        itemFingerprint.visibility = if (prefs.passwordEnabled) View.VISIBLE else View.GONE
        switchFingerprint.isChecked = prefs.fingerprintEnabled && canBio
        switchFingerprint.isEnabled = canBio
        tvFingerprintHint.visibility = if (!canBio) View.VISIBLE else View.GONE
        if (!canBio) switchFingerprint.isChecked = false

        val lockIdx = lockTimeValues.indexOf(prefs.lockTimeMinutes).takeIf { it >= 0 } ?: 0
        tvLockTimeValue.text = lockTimeLabels[lockIdx]

        switchAntiUninstall.isChecked = prefs.antiUninstallEnabled
        val realActive = isAdminActive()
        tvAntiUninstallHint.text = when {
            realActive -> "保护中 — 需停用设备管理员才能卸载"
            prefs.antiUninstallEnabled -> "状态异常 — 请重新激活"
            else -> "激活后可防止恶意卸载"
        }
        if (prefs.antiUninstallEnabled != realActive) {
            prefs.antiUninstallEnabled = realActive
            switchAntiUninstall.isChecked = realActive
        }

        refreshBackupInfo()
        refreshCrashCount()
    }

    private fun refreshBackupInfo() {
        tvLastBackup.text = prefs.lastBackupTime ?: "从未备份"
        val backupFiles = BackupManager.listBackupFiles(this)
        tvBackupCount.text = "${backupFiles.size}个"
    }

    private fun setupListeners() {
        switchPassword.setOnCheckedChangeListener { _, checked ->
            prefs.passwordEnabled = checked
            if (checked && prefs.password.isNullOrEmpty()) showSetPasswordDialog()
            refreshUI()
        }
        itemChangePwd.setOnClickListener { showSetPasswordDialog() }

        switchFingerprint.setOnCheckedChangeListener { _, checked ->
            prefs.fingerprintEnabled = checked
            Toast.makeText(this, if (checked) "指纹解锁已开启" else "指纹解锁已关闭", Toast.LENGTH_SHORT).show()
        }

        itemLockTime.setOnClickListener { showLockTimeDialog() }

        switchAntiUninstall.setOnCheckedChangeListener { _, checked ->
            if (checked) enableAntiUninstall() else disableAntiUninstall()
        }

        itemLocalBackup.setOnClickListener { showBackupOptionsDialog() }
        itemDbBackup.setOnClickListener { startDbBackup() }
        itemExportAll.setOnClickListener { exportAllData() }
        itemRestoreZip.setOnClickListener { pickZipForRestore() }
        itemPatientMgr.setOnClickListener { showPatientManager() }

        itemRestoreBackup.setOnClickListener { startRestore() }
        itemManageBackups.setOnClickListener { showManageBackupsDialog() }
        itemCreateShortcut.setOnClickListener {
            AlertDialog.Builder(this, R.style.RoundedDialog)
                .setTitle("快捷方式")
                .setItems(arrayOf("创建桌面快捷方式", "切换图标样式")) { _, which ->
                    if (which == 0) createShortcut() else switchIconStyle()
                }
                .setNegativeButton("取消", null)
                .show()
        }
        itemCrashLogs.setOnClickListener { exportCrashLogs() }
        itemIatConfig.setOnClickListener { showIatConfigDialog() }
        itemTianyiConfig.setOnClickListener { showTianyiConfigDialog() }
        itemQwenConfig.setOnClickListener { showQwenConfigDialog() }
        itemDeepseekConfig.setOnClickListener { showDeepseekConfigDialog() }
        itemShakeSensitive.setOnClickListener { showShakeSensitivityDialog() }
        itemStorage.setOnClickListener { showStorageDialog() }
        refreshStorageSize()

        // 摇一摇灵敏度
        refreshShakeSensitivity()

        // 启动时恢复状态（先设值再设监听器，避免初始值触发监听器）
        switchQuickRecord.isChecked = prefs.quickRecordEnabled
        if (prefs.quickRecordEnabled) {
            com.mynote.android.ui.recording.QuickRecordService.start(this)
        }

        switchQuickRecord.setOnCheckedChangeListener { _, checked ->
            prefs.quickRecordEnabled = checked
            if (checked) {
                com.mynote.android.ui.recording.QuickRecordService.start(this)
                Toast.makeText(this, "摇一摇快速录音已开启", Toast.LENGTH_SHORT).show()
            } else {
                com.mynote.android.ui.recording.QuickRecordService.stop(this)
                Toast.makeText(this, "摇一摇快速录音已关闭", Toast.LENGTH_SHORT).show()
            }
        }
        handleToolIntent()
    }

    private fun refreshShakeSensitivity() {
        tvShakeSensitive.text = if (prefs.shakeSensitive) "灵敏(摇3次)" else "迟钝(摇4次)"
    }

    // ===== 工具跳转 =====
    private fun handleToolIntent() {
        val target = intent.getStringExtra("open") ?: return
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed({
            when (target) {
                "disease" -> showDiseaseLookup()
                "drug" -> showDrugPickerFromSettings()
                "lab" -> showLabReference()
                "imaging" -> showImagingReference()
                "ecg" -> showEcgReference()
                "guide" -> showGuidelineLibrary()
                "pathway" -> showClinicalPathways()
                "calc" -> showMedicalCalculator()
                "abg" -> showAbgCalc()
                "drugInteract" -> showDrugInteraction()
                "aiLab" -> startAiLabReader()
                // 新增
                "emergency" -> showEmergencyProcedures()
                "abx" -> showAntibioticGuide()
                "transfusion" -> showTransfusionGuide()
                "tox" -> showToxicologyRef()
                "peds" -> showPediatricGrowth()
                "ivdrip" -> showIvDripCalc()
                "bsa" -> showBsaCalc()
                "pedidose" -> showPediDoseCalc()
                "trauma" -> showTraumaScore()
                "pain" -> showPainAssessment()
                "pregdrug" -> showPregDrugLookup()
                "scores" -> showClinicalScores()
            }
        }, 300)
    }
    private fun showDrugPickerFromSettings() {
        // 用药参考：直接打开疾病药品选择
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("用药参考")
            .setMessage("药品种类繁多，请使用搜索功能。\n\n设置中查看完整367种药品。")
            .setPositiveButton("打开完整药品库") { _, _ ->
                // Trigger existing drug lookup
                showDiseaseLookup() // falls through to drug reference
            }
            .setNegativeButton("关闭", null).show()
    }

    private fun showShakeSensitivityDialog() {
        val items = arrayOf("灵敏 (摇3次触发)", "迟钝 (摇4次触发)")
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("摇一摇灵敏度")
            .setItems(items) { _, which ->
                prefs.shakeSensitive = (which == 0)
                refreshShakeSensitivity()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ===== Password =====
    private fun showSetPasswordDialog() {
        val isNew = prefs.password.isNullOrEmpty()
        val view = layoutInflater.inflate(R.layout.dialog_set_password, null)
        val etPwd = view.findViewById<EditText>(R.id.et_password)
        val title = view.findViewById<TextView>(R.id.tv_dialog_title)
        title.text = if (isNew) "设置安全码" else "修改安全码"

        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setView(view)
            .setPositiveButton("确定") { _, _ ->
                val pwd = etPwd.text.toString().trim()
                if (pwd.length >= 3) {
                    prefs.password = pwd
                    prefs.passwordEnabled = true
                    switchPassword.isChecked = true
                    refreshUI()
                    Toast.makeText(this, "密码已设置", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "密码至少3位", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showLockTimeDialog() {
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("自动锁定时间")
            .setItems(lockTimeLabels.toTypedArray()) { _, which ->
                prefs.lockTimeMinutes = lockTimeValues[which]
                refreshUI()
            }
            .show()
    }

    // ===== Anti-Uninstall =====
    private fun initAntiUninstall() {
        dpm = getSystemService<DevicePolicyManager>()
        adminComponent = ComponentName(this, AdminReceiver::class.java)
    }

    private fun isAdminActive(): Boolean {
        return dpm?.isAdminActive(adminComponent ?: return false) == true
    }

    private fun enableAntiUninstall() {
        val component = adminComponent ?: return
        if (isAdminActive()) {
            prefs.antiUninstallEnabled = true
            refreshUI()
            return
        }
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "激活后可防止直接卸载 MyNote")
        }
        try {
            startActivity(intent)
            // onResume 中会检查激活状态并弹对话框
        } catch (e: Exception) {
            switchAntiUninstall.isChecked = false
            Toast.makeText(this, "设备不支持", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disableAntiUninstall() {
        val component = adminComponent
        if (component != null && dpm?.isAdminActive(component) == true) {
            AlertDialog.Builder(this, R.style.RoundedDialog)
                .setTitle("停用防卸载")
                .setMessage("停用后应用将自动锁定，需要密码才能访问数据。\n\n确定继续？")
                .setPositiveButton("停用") { _, _ ->
                    dpm?.removeActiveAdmin(component)
                    prefs.antiUninstallEnabled = false
                    IconManager.removeAllDynamicShortcuts(this@SettingsActivity)
                    switchAntiUninstall.isChecked = false
                    refreshUI()
                }
                .setNegativeButton("取消") { _, _ ->
                    switchAntiUninstall.isChecked = true
                }
                .show()
        } else {
            prefs.antiUninstallEnabled = false
            IconManager.removeAllDynamicShortcuts(this)
            switchAntiUninstall.isChecked = false
            refreshUI()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshBackupInfo()
        // 从系统激活界面返回后同步管理员真实状态
        val realActive = isAdminActive()
        val prefsActive = prefs.antiUninstallEnabled
        if (prefsActive != realActive) {
            prefs.antiUninstallEnabled = realActive
            switchAntiUninstall.isChecked = realActive
            tvAntiUninstallHint.text = if (realActive) "保护中 — 需停用设备管理员才能卸载" else "激活后可防止恶意卸载"
            if (realActive && !prefs.shortcutPromptDismissed) {
                // 首次从系统激活页返回，且用户未点过"暂不"
                showCreateShortcutPrompt()
            } else {
                IconManager.removeAllDynamicShortcuts(this@SettingsActivity)
            }
        }
    }

    /**
     * 防卸载激活后，弹出提示询问是否创建桌面快捷方式
     */
    private fun showCreateShortcutPrompt() {
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("桌面快捷方式")
            .setMessage("防卸载已激活！\n\n点击桌面图标将显示伪装计算器界面。\n建议创建「MN」快捷方式作为真实入口。\n\n是否现在创建？")
            .setPositiveButton("创建") { _, _ ->
                createShortcut()
            }
            .setNegativeButton("暂不") { _, _ ->
                prefs.shortcutPromptDismissed = true
                Toast.makeText(this, "可稍后在设置中点击「创建桌面快捷方式」", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    // ===== 桌面快捷方式 =====
    private fun createShortcut() {
        // 先选图标样式
        val icons = arrayOf(
            "默认图标" to R.mipmap.ic_launcher,
            "极简绿点" to R.drawable.ic_shortcut_minimal,
            "MN 文字" to R.drawable.ic_shortcut_mn
        )
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("选择图标样式")
            .setItems(icons.map { it.first }.toTypedArray()) { _, which ->
                createShortcutWithIcon(icons[which].second)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createShortcutWithIcon(iconRes: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            createShortcutLegacy()
            return
        }
        try {
            val shortcutManager = getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
            if (!shortcutManager.isRequestPinShortcutSupported) {
                Toast.makeText(this, "桌面不支持快捷方式固定", Toast.LENGTH_LONG).show()
                return
            }

            // 构建打开 LockActivity 的 Intent（带 from_shortcut 标记绕过伪装）
            val intent = Intent(this, LockActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("from_shortcut", true)
            }

            // 创建 ShortcutInfo（伪装名称，避免暴露 MyNote）
            val shortcut = ShortcutInfo.Builder(this, "mynote_open_${System.currentTimeMillis()}")
                .setShortLabel("MN")
                .setLongLabel("MN")
                .setIcon(Icon.createWithResource(this, iconRes))
                .setIntent(intent)
                .build()

            // 请求固定到桌面（会弹出确认弹窗）
            shortcutManager.requestPinShortcut(shortcut, null)
            Toast.makeText(this, "请确认添加到桌面", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "创建失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun createShortcutLegacy() {
        try {
            val intent = Intent(this, LockActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("from_shortcut", true)
            }
            val shortcutIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_NAME, "MN")
                putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this@SettingsActivity, R.mipmap.ic_launcher))
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
                putExtra("duplicate", false)
            }
            sendBroadcast(shortcutIntent)
            Toast.makeText(this, "已发送添加到桌面请求", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "创建失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== 备份选项 =====
    private fun showBackupOptionsDialog() {
        val configured = WebDAVBackup.isConfigured(this)
        val items = mutableListOf("📁 备份到本地文件",
            if (configured) "☁️ 坚果云备份（已配置 ✓）" else "☁️ 坚果云备份（未配置）",
            "⚙️ 配置坚果云 WebDAV",
            "📥 从坚果云恢复备份")

        AlertDialog.Builder(this)
            .setTitle("选择备份方式")
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> startJsonBackup()
                    1 -> showBackupCloudDialog()
                    2 -> showWebDAVConfigDialog()
                    3 -> showRestoreFromCloud()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    // ===== WebDAV 配置 =====
    private fun showWebDAVConfigDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }
        val etUrl = EditText(this).apply {
            hint = "WebDAV 地址"
            setText(prefs.webdavUrl.ifEmpty { "https://dav.jianguoyun.com/dav/" })
            setSingleLine()
        }
        val etUser = EditText(this).apply {
            hint = "用户名/邮箱"
            setText(prefs.webdavUser)
            setSingleLine()
        }
        val etPass = EditText(this).apply {
            hint = "应用密码（坚果云安全设置里生成）"
            setText(prefs.webdavPass)
            setSingleLine()
        }
        val hint = TextView(this).apply {
            text = "坚果云：dav.jianguoyun.com/dav/\n密码：坚果云网页版→安全设置→第三方应用密码"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(0xFF666666.toInt())
            setPadding(0, 8, 0, 0)
        }
        layout.addView(etUrl)
        layout.addView(etUser)
        layout.addView(etPass)
        layout.addView(hint)

        AlertDialog.Builder(this)
            .setTitle("⚙️ 坚果云 WebDAV 配置")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                prefs.webdavUrl = etUrl.text.toString().trim()
                prefs.webdavUser = etUser.text.toString().trim()
                prefs.webdavPass = etPass.text.toString().trim()
                Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ===== 云备份 =====
    private fun showBackupCloudDialog() {
        val configured = WebDAVBackup.isConfigured(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }

        val statusText = TextView(this).apply {
            text = if (configured) "✅ 已配置坚果云 WebDAV\n备份目录: /MyNote/" else "⚠️ 未配置"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, 0, 0, 12)
        }
        val switchSync = SwitchCompat(this).apply {
            isChecked = prefs.backupAutoBackup
        }
        val switchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
        }
        switchRow.addView(TextView(this).apply {
            text = "保存时自动备份到坚果云"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        switchRow.addView(switchSync)

        layout.addView(statusText)
        layout.addView(switchRow)

        val builder = AlertDialog.Builder(this)
            .setTitle("☁️ 坚果云备份")
            .setView(layout)
            .setPositiveButton("保存设置") { _, _ ->
                prefs.backupAutoBackup = switchSync.isChecked
                Toast.makeText(this, if (switchSync.isChecked) "自动云备份已启用" else "已关闭", Toast.LENGTH_SHORT).show()
            }

        if (!configured) {
            builder.setNeutralButton("去配置") { _, _ -> showWebDAVConfigDialog() }
        } else {
            builder.setNeutralButton("立即备份") { _, _ ->
                lifecycleScope.launch {
                    Toast.makeText(this@SettingsActivity, "正在上传到坚果云...", Toast.LENGTH_SHORT).show()
                    WebDAVBackup.uploadBackup(this@SettingsActivity) { ok, msg ->
                        runOnUiThread { Toast.makeText(this@SettingsActivity, if (ok) "✅ $msg" else "❌ $msg", Toast.LENGTH_LONG).show() }
                    }
                }
            }
        }
        builder.setNegativeButton("取消", null).show()
    }

    // ===== 从云端恢复 =====
    private fun showRestoreFromCloud() {
        lifecycleScope.launch {
            Toast.makeText(this@SettingsActivity, "正在从坚果云读取备份列表...", Toast.LENGTH_SHORT).show()
            val files = WebDAVBackup.listBackupFiles(this@SettingsActivity)
            runOnUiThread {
                if (files.isEmpty()) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("📥 云端备份列表")
                        .setMessage("坚果云 /MyNote/ 下暂无备份文件")
                        .setPositiveButton("确定", null)
                        .show()
                    return@runOnUiThread
                }
                val labels = files.map { "${it.name}  (${it.time} · ${formatFileSize(it.size)})" }.toTypedArray()
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("📥 云端备份列表 (${files.size}个)")
                    .setItems(labels) { _, which ->
                        val f = files[which]
                        AlertDialog.Builder(this@SettingsActivity)
                            .setTitle("确认恢复")
                            .setMessage("将从云端下载并恢复:\n${f.name}\n\n⚠️ 当前数据将被覆盖")
                            .setPositiveButton("确认恢复") { _, _ ->
                                lifecycleScope.launch {
                                    Toast.makeText(this@SettingsActivity, "正在下载备份...", Toast.LENGTH_SHORT).show()
                                    val json = WebDAVBackup.downloadBackup(this@SettingsActivity, f.path)
                                    if (json != null) {
                                        runOnUiThread {
                                            AlertDialog.Builder(this@SettingsActivity)
                                                .setTitle("恢复方式")
                                                .setItems(arrayOf("覆盖当前数据（清空后导入）", "追加到当前数据")) { _, mode ->
                                                    lifecycleScope.launch {
                                                        try {
                                                            val ok = BackupManager.importJsonString(this@SettingsActivity, json, mode == 0)
                                                            runOnUiThread {
                                                                Toast.makeText(this@SettingsActivity, if (ok) "✅ 恢复成功" else "❌ 恢复失败", Toast.LENGTH_SHORT).show()
                                                                refreshBackupInfo()
                                                            }
                                                        } catch (e: Exception) {
                                                            runOnUiThread { Toast.makeText(this@SettingsActivity, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show() }
                                                        }
                                                    }
                                                }
                                                .setNegativeButton("取消", null)
                                                .show()
                                        }
                                    } else {
                                        runOnUiThread { Toast.makeText(this@SettingsActivity, "下载失败", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }

    private fun formatCloudSize(size: Long): String = when {
        size >= 1_048_576 -> "%.1fMB".format(size / 1_048_576.0)
        size >= 1024 -> "%.1fKB".format(size / 1024.0)
        else -> "${size}B"
    }

    // ===== JSON 完整备份 =====
    private fun startJsonBackup() {
        val fileName = "MyNote_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
        jsonExportLauncher.launch(fileName)
    }

    private fun exportJsonBackup(uri: Uri) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    BackupManager.createJsonBackup(this@SettingsActivity, uri)
                }
                when (result) {
                    is BackupManager.BackupResult.Success -> {
                        updateLastBackupTime()
                        Toast.makeText(this@SettingsActivity,
                            "备份成功！${result.noteCount}篇笔记, ${result.itemCount}个内容项, ${result.catCount}个分类",
                            Toast.LENGTH_LONG).show()
                    }
                    is BackupManager.BackupResult.Error ->
                        Toast.makeText(this@SettingsActivity, "备份失败: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                refreshBackupInfo()
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@SettingsActivity, "备份失败: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // ===== 新增知识库 =====
    private fun showEmergencyProcedures() = showKnowledgeList("急救流程", com.mynote.android.util.EmergencyProcedures.all.map {
        it.title to "${it.indication}\n\n${it.steps}"}, com.mynote.android.util.EmergencyProcedures::search)
    private fun showAntibioticGuide() = showKnowledgeList("抗菌药物选药", com.mynote.android.util.AntibioticGuide.all.map {
        it.title to "病原: ${it.pathogen}\n\n一线: ${it.firstLine}\n\n替代: ${it.alternative}"}, com.mynote.android.util.AntibioticGuide::search)
    private fun showTransfusionGuide() = showKnowledgeList("输血指征", com.mynote.android.util.TransfusionGuide.all.map {
        it.title to "指征: ${it.threshold}\n\n备注: ${it.notes}"}, com.mynote.android.util.TransfusionGuide::search)
    private fun showToxicologyRef() = showKnowledgeList("中毒与解毒", com.mynote.android.util.ToxicologyRef.all.map {
        it.agent to "毒症: ${it.toxidrome}\n\n解毒: ${it.antidote}\n\n要点: ${it.keyPoints}"}, com.mynote.android.util.ToxicologyRef::search, mapOf(
        "毒蘑菇(鹅膏菌)" to "https://cn.bing.com/images/search?q=%E6%AF%92%E8%98%91%E8%8F%87+%E9%B9%85%E8%86%8F%E8%8F%8C",
        "乌头/附子" to "https://cn.bing.com/images/search?q=%E4%B9%8C%E5%A4%B4+%E9%99%84%E5%AD%90+%E6%A4%8D%E7%89%A9",
        "断肠草/钩吻" to "https://cn.bing.com/images/search?q=%E6%96%AD%E8%82%A0%E8%8D%89+%E9%92%A9%E5%90%BB+%E6%A4%8D%E7%89%A9",
    ))
    private fun showPediatricGrowth() = showKnowledgeList("儿童生长发育", com.mynote.android.util.PediatricGrowth.all.map {
        it.title to "年龄段: ${it.ageRange}\n${it.normalData}"}, com.mynote.android.util.PediatricGrowth::search, mapOf(
        "身高体重百分位(WHO 2006)" to "https://cn.bing.com/images/search?q=%E5%84%BF%E7%AB%A5%E8%BA%AB%E9%AB%98%E4%BD%93%E9%87%8D%E7%99%BE%E5%88%86%E4%BD%8D%E6%9B%B2%E7%BA%BF+WHO",
        "头围参考值(WHO)" to "https://cn.bing.com/images/search?q=%E5%84%BF%E7%AB%A5%E5%A4%B4%E5%9B%B4%E7%99%BE%E5%88%86%E4%BD%8D%E6%9B%B2%E7%BA%BF",
    ))

    // ===== 新增临床工具 =====
    private fun showIvDripCalc() = showCalcDialog("输液速度计算", """
        |请输入: 液体总量(mL) × 滴系数(drop/mL) × 时间(分钟)
        |公式: 滴/分 = 总量×滴系数/分钟
    """.trimMargin(), listOf(Triple("volume", "总液体量(mL)", "500"), Triple("dropFactor", "滴系数(滴/mL)", "20"), Triple("timeMin", "时间(分钟)", "60")),
    { v -> val r = com.mynote.android.util.MedicalCalculator.ivDrip(v[0].toDouble(), v[1].toInt(), v[2].toDouble()); "=${r.dropsPerMin.toInt()}滴/分，${r.mlPerHour.toInt()}mL/h" })

    private fun showBsaCalc() = showCalcDialog("体表面积 BSA", "DuBois 公式: BSA=0.007184×WT^0.425×HT^0.725",
        listOf(Triple("weight", "体重(kg)", "70"), Triple("height", "身高(cm)", "170")),
    { v -> val r = com.mynote.android.util.MedicalCalculator.bsa(v[0].toDouble(), v[1].toDouble()); "BSA=${r.bsa}m² (${r.formula})" })

    private fun showPediDoseCalc() = showCalcDialog("儿童剂量速算", "按体重给药: 体重(kg) × 剂量(mg/kg) ÷ 药物浓度(mg/mL)",
        listOf(Triple("weight", "体重(kg)", "10"), Triple("mgPerKg", "剂量(mg/kg)", "15"), Triple("conc", "浓度(mg/mL)", "50")),
    { v -> val r = com.mynote.android.util.MedicalCalculator.pediDose(v[0].toDouble(), v[1].toDouble(), v[2].toDouble()); r.summary })

    private fun showClinicalScores() = showKnowledgeList("临床常用量表", com.mynote.android.util.ClinicalScores.all.map {
        it.title to "[${it.indication}]\n\n${it.content}"}, com.mynote.android.util.ClinicalScores::search, mapOf(
        "皮肤病损形态学速查" to "https://cn.bing.com/images/search?q=%E7%9A%AE%E8%82%A4%E7%97%85%E6%8D%9F+%E6%96%91%E7%96%B9+%E4%B8%98%E7%96%B9+%E6%B0%B4%E7%96%B1+%E8%8A%82%E7%BB%93+%E9%A3%8E%E5%9B%A2",
    ))

    private fun showTraumaScore() {
        val items = listOf("ISS 创伤严重度评分" to {
            // GCS + AIS zones dialog
            val zones = listOf("头部/颈部" to 1, "面部" to 2, "胸部" to 3, "腹部/盆腔" to 4, "四肢/骨盆" to 5, "体表" to 6)
            val selected = mutableMapOf<Int, Int>()
            val et = android.widget.EditText(this).apply { hint = "各区域AIS分数,用逗号分隔\n例: 4,3,2"; setSingleLine() }
            AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("ISS 创伤严重度").setView(et)
                .setMessage("输入各受伤区域的AIS分数(AIS 1-6),逗号分隔。\nISS = 最高的三个AIS²之和")
                .setPositiveButton("计算") { _, _ ->
                    val aisStr = et.text.toString().split(",", "，").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..6 }
                    if (aisStr.size >= 1) {
                        val r = com.mynote.android.util.MedicalCalculator.iss(aisStr)
                        val msg = "ISS=${r.iss}分\n${r.risk}\n${r.mortality}"
                        AlertDialog.Builder(this@SettingsActivity, R.style.RoundedDialog).setTitle("ISS 创伤严重度")
                            .setMessage(msg).setPositiveButton("确定", null)
                            .setNeutralButton("📋 复制") { _, _ ->
                                getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(
                                    android.content.ClipData.newPlainText("iss_score", msg))
                                Toast.makeText(this@SettingsActivity, "已复制", Toast.LENGTH_SHORT).show()
                            }.show()
                    }
                }.setNegativeButton("取消", null).show()
        }, "RTS 改良创伤评分" to { showTraumaRts() })
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("创伤评分").setItems(items.map { it.first }.toTypedArray()) { _, i -> items[i].second() }
            .setNegativeButton("关闭", null).show()
    }

    private fun showTraumaRts() {
        val etGcs = android.widget.EditText(this).apply { hint = "GCS"; setSingleLine(); inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etSbp = android.widget.EditText(this).apply { hint = "收缩压(mmHg)"; setSingleLine(); inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etRr = android.widget.EditText(this).apply { hint = "呼吸频率(/min)"; setSingleLine(); inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 10) }
        layout.addView(etGcs); layout.addView(etSbp); layout.addView(etRr)
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("RTS 改良创伤评分").setView(layout)
            .setPositiveButton("计算") { _, _ ->
                val g = etGcs.text.toString().toIntOrNull() ?: 15; val s = etSbp.text.toString().toIntOrNull() ?: 120; val r = etRr.text.toString().toIntOrNull() ?: 20
                val result = com.mynote.android.util.MedicalCalculator.rts(g, s, r)
                val rtMsg = "RTS=${result.rts}\n${result.coded}\n${result.surv}"
                AlertDialog.Builder(this@SettingsActivity, R.style.RoundedDialog).setTitle("RTS 改良创伤评分")
                    .setMessage(rtMsg).setPositiveButton("确定", null)
                    .setNeutralButton("📋 复制") { _, _ ->
                        getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(
                            android.content.ClipData.newPlainText("rts_score", rtMsg))
                        Toast.makeText(this@SettingsActivity, "已复制", Toast.LENGTH_SHORT).show()
                    }.show()
            }.setNegativeButton("取消", null).show()
    }

    private fun showPainAssessment() {
        val scores = (0..10).map { "$it 分" }.toTypedArray()
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("疼痛评估 NRS").setItems(scores) { _, i ->
            val r = com.mynote.android.util.MedicalCalculator.painAssess(i)
            val painMsg = "NRS ${r.nrs}分 — ${r.level}\n处理建议:\n${r.mgmt}"
            AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("NRS ${r.nrs}分 — ${r.level}")
                .setMessage("处理建议:\n${r.mgmt}").setPositiveButton("确定", null)
                .setNeutralButton("📋 复制") { _, _ ->
                    getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(
                        android.content.ClipData.newPlainText("pain_score", painMsg))
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                }.show()
        }.setNegativeButton("关闭", null).show()
    }

    private fun showPregDrugLookup() {
        val et = android.widget.EditText(this).apply { hint = "输入药品名 (如: 卡托普利、阿莫西林...)"; setSingleLine(); textSize = 14f }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 10) }
        val tvCat = TextView(this).apply { text = "A=安全 B=较安全 C=权衡 D=慎用 X=禁忌"; textSize = 11f; setTextColor(Color.GRAY); setPadding(0, 4, 0, 0) }
        layout.addView(et); layout.addView(tvCat)
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("妊娠用药分级(FDA)").setView(layout)
            .setPositiveButton("查询") { _, _ ->
                val name = et.text.toString().trim()
                val r = com.mynote.android.util.MedicalCalculator.pregnancyDrug(name)
                if (r != null) {
                    val catInfo = com.mynote.android.util.MedicalCalculator.pregnancyCategories.firstOrNull { it.first == r.cat.take(1) }?.second ?: ""
                    AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("${r.drug} → ${r.cat}类")
                        .setMessage("$catInfo\n\nA=安全 B=较安全 C=权衡利弊 D=慎用(危及生命) X=禁忌")
                        .setPositiveButton("确定", null).show()
                } else {
                    Toast.makeText(this, "未找到药品\"$name\"，尝试其他名称", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("关闭", null).show()
    }

    /** 通用知识列表对话框 */
    private fun <T> showKnowledgeList(title: String, items: List<Pair<String, String>>, searchFn: ((String) -> List<T>)? = null, urls: Map<String, String> = emptyMap()) {
        val et = android.widget.EditText(this).apply { hint = "搜索..."; setSingleLine(); textSize = 14f }
        var tvCount = TextView(this).apply { text = "共${items.size}项"; textSize = 11f; setTextColor(Color.GRAY); setPadding(0, 8, 0, 0) }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 12, 20, 0) }
        root.addView(et); root.addView(tvCount)

        var dlg: AlertDialog? = null
        fun build(list: List<Pair<String, String>>) {
            dlg?.dismiss()
            val labels = list.map { it.first }.toTypedArray()
            dlg = AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(title).setView(root)
                .setItems(labels) { _, i2 ->
                    val itemTitle = list[i2].first; val itemContent = list[i2].second
                    val builder = AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(itemTitle)
                        .setMessage(itemContent)
                        .setPositiveButton("确定", null)
                        .setNeutralButton("📋 复制") { _, _ ->
                            val clipData = android.content.ClipData.newPlainText("clinical_tool", "$itemTitle\n$itemContent")
                            getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(clipData)
                            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }
                    urls[itemTitle]?.let { url ->
                        builder.setNegativeButton("📊 看图") { _, _ ->
                            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        }
                    }
                    builder.show()
                }.setNegativeButton("关闭", null).create()
            dlg!!.show()
        }
        build(items)
        if (searchFn != null) {
            et.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    val q = s.toString().trim()
                    if (q.isEmpty()) { build(items); tvCount.text = "共${items.size}项"; return }
                    val results = searchFn(q) as? List<Any> ?: emptyList()
                    // Rebuild from original items by matching first field
                    val filtered = items.filter { (title, _) -> title.lowercase().contains(q.lowercase()) || items.any { it.first == title && it.second.lowercase().contains(q.lowercase()) } }
                    tvCount.text = "找到${filtered.size}/${items.size}项"; build(filtered)
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, af: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })
        }
    }

    /** 通用计算器对话框 — 结果内嵌可修改重算，记忆上次数值 */
    private fun showCalcDialog(title: String, hint: String, fields: List<Triple<String, String, String>>, calc: (List<String>) -> String) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 10) }
        // 记忆上次输入
        val memSp = getSharedPreferences("mynote_calc", Context.MODE_PRIVATE)
        val memKey = "calc_mem_${title}"
        val saved = memSp.getString(memKey, null)
        val savedVals = saved?.split("|")?.take(fields.size) ?: emptyList()
        val ets = mutableListOf<android.widget.EditText>()
        for ((i, field) in fields.withIndex()) {
            val (_, label, defVal) = field
            val et = android.widget.EditText(this)
            et.hint = label
            et.setText(if (i < savedVals.size) savedVals[i] else defVal)
            et.setSingleLine()
            et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL)
            layout.addView(et); ets.add(et)
        }
        // 提示文字
        layout.addView(TextView(this).apply { text = hint; textSize = 11f; setTextColor(Color.GRAY); setPadding(0, 8, 0, 4) })
        // 结果区域
        val tvResult = TextView(this).apply {
            textSize = 15f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1565C0")); setPadding(0, 10, 0, 6); visibility = android.view.View.GONE
        }
        layout.addView(tvResult)
        // 计算按钮行
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 0) }
        val btnCalc = android.widget.Button(this).apply {
            text = "计算"; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1976D2"))
            textSize = 14f; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnCopy = android.widget.Button(this).apply {
            text = "📋 复制"; setTextColor(Color.parseColor("#1976D2"))
            textSize = 13f; visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(8, 0, 0, 0) }
            setBackgroundColor(Color.parseColor("#E3F2FD"))
        }
        btnRow.addView(btnCalc); btnRow.addView(btnCopy)
        layout.addView(btnRow)
        // 历史记录
        val histSp = getSharedPreferences("calc_history", Context.MODE_PRIVATE)
        val btnHist = TextView(this).apply {
            text = "📜 历史记录"; textSize = 11f; setTextColor(Color.parseColor("#78909C"))
            setPadding(0, 8, 0, 0); visibility = View.GONE
        }
        layout.addView(btnHist)

        var lastResult = ""
        btnCalc.setOnClickListener {
            val vals = ets.map { it.text.toString().ifEmpty { "0" } }
            lastResult = calc(vals)
            tvResult.text = lastResult; tvResult.visibility = View.VISIBLE
            btnCopy.visibility = View.VISIBLE
            // 记忆数值
            memSp.edit().putString(memKey, vals.joinToString("|")).apply()
            // 保存计算历史
            val ts = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val entry = "$ts $lastResult"
            val hist = histSp.getString("list", "")?.split("||")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            hist.add(0, entry); if (hist.size > 20) hist.removeAt(hist.size - 1)
            histSp.edit().putString("list", hist.joinToString("||")).apply()
            btnHist.visibility = View.VISIBLE
            btnHist.text = "📜 历史记录 (${hist.size})"
        }
        btnCopy.setOnClickListener {
            getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(
                android.content.ClipData.newPlainText("calc_result", "$title\n$lastResult"))
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
        }
        btnHist.setOnClickListener {
            val hist = histSp.getString("list", "")?.split("||")?.filter { it.isNotBlank() } ?: emptyList()
            if (hist.isEmpty()) { Toast.makeText(this, "暂无记录", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("计算历史 (最近20条)")
                .setItems(hist.toTypedArray(), null).setPositiveButton("关闭", null).show()
        }

        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(title).setView(layout)
            .setPositiveButton("关闭", null).show()
    }

    // ===== 医学计算器 =====
    private fun showMedicalCalculator() {
        val cats = listOf(
            "【心/血管】" to listOf("CHA₂DS₂-VASc 房颤卒中" to ::showChadsVascCalc, "HAS-BLED 抗凝出血" to ::showHasBledCalc, "HEART 胸痛分层" to ::showHeartCalc, "GRACE 2.0 ACS风险" to ::showGraceCalc, "TIMI UA/NSTEMI" to ::showTimiCalc, "Framingham 10年CVD" to ::showFraminghamCalc, "Wells DVT 深静脉血栓" to ::showWellsDvtCalc, "Wells PE + PERC 肺栓塞" to ::showWellsPeCalc),
            "【呼吸】" to listOf("CURB-65 肺炎严重度" to ::showCurb65Calc),
            "【肾内】" to listOf("eGFR CKD-EPI 2021" to ::showEgfrCalc, "矫正血钙 + 阴离子间隙" to ::showCalcCaAg),
            "【消化】" to listOf("MELD 肝病严重度" to ::showMeldCalc, "Child-Pugh 肝硬化分级" to ::showChildPughCalc),
            "【神内】" to listOf("GCS 格拉斯哥昏迷" to ::showGcsCalc, "ABCD² TIA卒中风险" to ::showAbcd2Calc),
            "【ICU/感染】" to listOf("qSOFA 脓毒症筛查" to ::showQsofaCalc, "SOFA 序贯器官衰竭" to ::showSofaCalc, "APACHE II 重症评分" to ::showApacheCalc, "NEWS2 早期预警" to ::showNews2Calc, "Padua 血栓风险" to ::showPaduaCalc),
            "【ABG 血气分析】" to listOf("血气自动判读" to ::showAbgCalc),
            "【消化/外科】" to listOf("Ranson 胰腺炎评分" to ::showRansonCalc, "Alvarado 阑尾炎评分" to ::showAlvaradoCalc),
            "【ENT/呼吸】" to listOf("Centor 咽炎评分" to ::showCentorCalc, "Geneva PE 肺栓塞" to ::showGenevaPeCalc),
            "【内分泌】" to listOf("HOMA-IR 胰岛素抵抗" to ::showHomaCalc),
            "【风湿】" to listOf("DAS28 类风湿活动度" to ::showDas28Calc),
            "【妇产】" to listOf("Bishop 宫颈成熟度" to ::showBishopCalc, "APGAR 新生儿评分" to ::showApgarCalc),
            "【通用】" to listOf("BMI 体重指数" to ::showBmiCalc, "补钠量" to ::showNaDeficitCalc, "补钾量" to ::showKDeficitCalc, "维持补液量 4-2-1" to ::showMaintenanceFluidCalc, "血渗透压" to ::showSerumOsmCalc, "激素等效换算" to ::showSteroidCalc),
        )

        // 计算历史
        val histBtn = TextView(this).apply {
            text = "\n📋 计算历史 (${calcHistory.size}条)"
            textSize = 14f; setTextColor(Color.parseColor("#1565C0"))
            setPadding(0, 8, 0, 0); gravity = android.view.Gravity.CENTER
            setOnClickListener { showCalcHistory() }
        }

        val items = cats.map { it.first }.toMutableList()
        val actions = cats.map { cat ->
            {
                val subs = cat.second
                AlertDialog.Builder(this, R.style.RoundedDialog)
                    .setTitle(cat.first)
                    .setItems(subs.map { it.first }.toTypedArray()) { _, i2 -> subs[i2].second() }
                    .setNegativeButton("返回") { _, _ -> showMedicalCalculator() }
                    .show()
            }
        }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 8, 20, 8) }
        root.addView(histBtn)
        val dlg = AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("医学计算器 (${cats.sumOf { it.second.size }}项)")
            .setView(root)
            .setItems(items.toTypedArray()) { _, i -> actions[i]() }
            .setNegativeButton("关闭", null).create()
        dlg.show()
        dlg.window?.setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    // 计算历史
    private val calcHistory: MutableList<String>
        get() = prefs.calcHistoryList()

    private fun saveCalcResult(title: String, result: String) {
        prefs.addCalcHistory("$title\n$result")
    }

    private fun showCalcHistory() {
        val list = calcHistory.toList().reversed()
        if (list.isEmpty()) {
            Toast.makeText(this, "暂无计算历史", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("计算历史 (最近${list.size}条)")
            .setItems(list.take(20).toTypedArray(), null)
            .setPositiveButton("清空") { _, _ -> prefs.clearCalcHistory(); showMedicalCalculator() }
            .setNegativeButton("返回") { _, _ -> showMedicalCalculator() }
            .show()
    }

    private fun showCalcResult(title: String, result: String) {
        val full = "$title\n$result"
        val msg = TextView(this).apply {
            text = full; textSize = 15f; setPadding(32, 16, 32, 16)
            // 风险颜色：❌=红, ⚠️=黄, 其他=正常
            when {
                result.contains("❌") || result.contains("高危") || result.contains("极高危") || result.contains("死亡率>") -> {
                    setTextColor(Color.RED); setBackgroundColor(Color.argb(25, 255, 0, 0))
                }
                result.contains("⚠️") || result.contains("中危") || result.contains("谨慎") -> {
                    val c = Color.parseColor("#E65100"); setTextColor(c); setBackgroundColor(Color.argb(25, 230, 81, 0))
                }
            }
        }
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("计算结果").setView(msg)
            .setPositiveButton("确定", null)
            .setNeutralButton("📋 复制") { _, _ ->
                val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("calc", full))
                saveCalcResult(title, result)
                Toast.makeText(this, "已复制并保存到历史", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun calcInputDialog(title: String, fields: List<Pair<String, String>>, calc: (List<String>) -> String) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 16, 40, 0) }
        val ets = mutableListOf<EditText>()
        for ((label, hint) in fields) {
            root.addView(TextView(this).apply { text = label; textSize = 13f; setPadding(0, 8, 0, 2) })
            val et = EditText(this).apply { this.hint = hint; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
            ets.add(et); root.addView(et)
        }
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(title).setView(root)
            .setPositiveButton("计算") { _, _ ->
                showCalcResult(title, calc(ets.map { it.text.toString().trim() }))
            }.setNegativeButton("取消", null).show()
    }

    private fun showCheckboxCalc(title: String, items: List<String>, calc: (List<Int>) -> String) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 16, 40, 0) }
        val cb = mutableListOf<android.widget.CheckBox>()
        for (it in items) { val c = android.widget.CheckBox(this).apply { text = it }; cb.add(c); root.addView(c) }
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(title).setView(root)
            .setPositiveButton("计算") { _, _ ->
                showCalcResult(title, calc(cb.map { if (it.isChecked) 1 else 0 }))
            }.setNegativeButton("取消", null).show()
    }

    private fun showEgfrCalc() { calcInputDialog("eGFR", listOf("年龄" to "", "肌酐 mg/dL" to "", "性别(0男1女)" to "")) { v -> val r = MedicalCalculator.egfr(v[0].toInt(), v[1].toDouble(), v[2] == "1"); "eGFR: %.0f\n%s\n%s".format(r.egfr, r.stage, r.interpretation) } }
    private fun showNaDeficitCalc() { calcInputDialog("补钠量", listOf("体重 kg" to "", "目标Na" to "", "当前Na" to "")) { v -> val w = v[0].toDouble(); val d = MedicalCalculator.sodiumDeficit(w, v[1].toInt(), v[2].toInt()); "缺Na: %.0f mmol\n≈ %.0f mL 3%%NaCl".format(d, d/0.513) } }
    private fun showMaintenanceFluidCalc() { calcInputDialog("维持补液", listOf("体重 kg" to "")) { v -> val w = v[0].toDouble(); val m = MedicalCalculator.maintenanceFluid(w); "24h: %.0f mL\n速率: %.0f mL/h".format(m, m/24) } }
    private fun showBmiCalc() { calcInputDialog("BMI", listOf("体重 kg" to "", "身高 cm" to "")) { v -> val r = MedicalCalculator.bmi(v[0].toDouble(), v[1].toDouble()); "BMI: %.1f\n%s".format(r.bmi, r.category) } }

    private fun showChadsVascCalc() { showCheckboxCalc("CHA₂DS₂-VASc", listOf("心衰/EF≤40%", "高血压", "年龄≥75(2分)", "年龄65-74", "糖尿病", "卒中/TIA(2分)", "血管疾病", "女性")) { v -> val r = MedicalCalculator.chadsVasc(v[0]==1,v[1]==1,v[2]==1,v[3]==1,v[4]==1,v[5]==1,v[6]==1,v[7]==1); "=${r.score}分 ${r.risk}\n${r.recommendation}" } }
    private fun showHasBledCalc() { showCheckboxCalc("HAS-BLED", listOf("高血压", "肾功能异常", "肝功能异常", "卒中史", "出血史", "INR不稳定", "年龄>65", "抗血小板/NSAIDs", "酗酒")) { v -> val r = MedicalCalculator.hasBled(v[0]==1,v[1]==1,v[2]==1,v[3]==1,v[4]==1,v[5]==1,v[6]==1,v[7]==1,v[8]==1); "=${r.score}分\n${r.risk}" } }
    private fun showCurb65Calc() { showCheckboxCalc("CURB-65", listOf("意识障碍", "BUN>7mmol/L", "呼吸≥30", "收缩压<90", "年龄≥65")) { v -> val r = MedicalCalculator.curb65(v[0]==1,v[1]==1,v[2]==1,v[3]==1,v[4]==1); "=${r.score}分 死亡率${r.mortality}\n${r.site}" } }
    private fun showWellsDvtCalc() { showCheckboxCalc("Wells DVT", listOf("活动性肿瘤", "瘫痪/石膏", "卧床>3天", "深静脉压痛", "全下肢肿胀", "小腿肿胀>3cm", "凹陷性水肿", "浅静脉侧支", "其他诊断可能小(-2)")) { v -> val r = MedicalCalculator.wellsDvt(v[0]==1,v[1]==1,v[2]==1,v[3]==1,v[4]==1,v[5]==1,v[6]==1,v[7]==1,v[8]==1); "=${r.score}分 ${r.risk}\n${r.action}" } }
    private fun showHeartCalc() { showCheckboxCalc("HEART", listOf("病史 高2/中1/低0", "ECG ST↓2/非特异1/正常0", "年龄>65=2/45-65=1/<45=0", "危险≥3=2/1-2=1/0=0", "TnI>3×=2/1-3×=1/≤1=0")) { v -> val r = MedicalCalculator.heart(v[0],v[1],v[2],v[3],v[4]); "=${r.score}分 ${r.risk}\n${r.action}" } }

    // new calculators
    private fun showGraceCalc() { calcInputDialog("GRACE 2.0", listOf("年龄" to "", "心率 次/分" to "", "收缩压 mmHg" to "", "肌酐 mg/dL" to "", "Killip 1-4" to "", "心脏骤停 0/1" to "", "ST↓ 0/1" to "", "肌钙蛋白↑ 0/1" to "")) { v -> val r = MedicalCalculator.grace(v[0].toInt(),v[1].toInt(),v[2].toInt(),v[3].toDouble(),v[4].toInt(),v[5]=="1",v[6]=="1",v[7]=="1"); "GRACE=${r.score} 住院死亡${r.inhospMortality} ${r.risk}" } }
    private fun showTimiCalc() { showCheckboxCalc("TIMI UA/NSTEMI", listOf("年龄≥65", "≥3项CAD危险因素", "已知CAD狭窄≥50%", "7天用过ASA", "24h内≥2次心绞痛", "ST段偏移≥0.5mm", "肌钙蛋白升高")) { v -> val r = MedicalCalculator.timi(v[0]==1,v[1]==1,v[2]==1,v[3]==1,v[4]==1,v[5]==1,v[6]==1); "TIMI=${r.score} 14d事件率${r.risk14d}" } }
    private fun showFraminghamCalc() { calcInputDialog("Framingham 10年CVD", listOf("年龄" to "", "总胆固醇 mmol/L" to "", "HDL mmol/L" to "", "收缩压 mmHg" to "", "正服降压药 0/1" to "", "吸烟 0/1" to "", "糖尿病 0/1" to "")) { v -> MedicalCalculator.framingham(v[0].toInt(),v[1].toDouble(),v[2].toDouble(),v[3].toInt(),v[4]=="1",v[5]=="1",v[6]=="1") } }
    private fun showWellsPeCalc() { showCheckboxCalc("Wells PE", listOf("DVT体征 3分", "PE可能性最大 3分", "心率>100 1.5分", "制动/手术 1.5分", "既往PE/DVT 1.5分", "咯血 1分", "肿瘤 1分")) { v -> val r = MedicalCalculator.wellsPe(v[0]==1,v[1]==1,v[2]==1,v[3]==1,v[4]==1,v[5]==1,v[6]==1); "=${r.score} ${r.probability}\n${r.action}" } }
    private fun showGcsCalc() { calcInputDialog("GCS 格拉斯哥", listOf("睁眼 4/3/2/1" to "", "语言 5/4/3/2/1" to "", "运动 6/5/4/3/2/1" to "")) { v -> val r = MedicalCalculator.gcs(v[0].toInt(),v[1].toInt(),v[2].toInt()); "GCS=${r.total} ${r.level}" } }
    private fun showMeldCalc() { calcInputDialog("MELD", listOf("胆红素 mg/dL" to "", "INR" to "", "肌酐 mg/dL" to "", "透析 0/1" to "")) { v -> "MELD=${MedicalCalculator.meld(v[0].toDouble(),v[1].toDouble(),v[2].toDouble(),v[3]=="1")}" } }
    private fun showChildPughCalc() { calcInputDialog("Child-Pugh", listOf("胆红素 µmol/L" to "", "白蛋白 g/L" to "", "INR" to "", "腹水 1/2/3" to "", "肝脑 1/2/3" to "")) { v -> val r = MedicalCalculator.childPugh(v[0].toDouble(),v[1].toDouble(),v[2].toDouble(),v[3].toInt(),v[4].toInt()); "=${r.score}分 ${r.grade}\n${r.survival}" } }
    private fun showQsofaCalc() { showCheckboxCalc("qSOFA", listOf("呼吸≥22次/分", "收缩压≤100", "GCS<15")) { v -> MedicalCalculator.qsofa(v[0]==1,v[1]==1,v[2]==1) } }
    private fun showAbcd2Calc() { calcInputDialog("ABCD²", listOf("年龄>60 0/1" to "", "BP≥140/90 0/1" to "", "临床症状 0/1/2" to "", "持续时间 0/1/2" to "", "糖尿病 0/1" to "")) { v -> val r = MedicalCalculator.abcd2(v[0]=="1",v[1]=="1",v[2].toInt(),v[3].toInt(),v[4]=="1"); "ABCD²=${r.score} 2d卒中${r.risk2d}\n${r.action}" } }
    private fun showDas28Calc() { calcInputDialog("DAS28-ESR", listOf("压痛28" to "", "肿胀28" to "", "ESR mm/h" to "", "VAS 疼痛 0-100" to "")) { v -> MedicalCalculator.das28(v[0].toInt(),v[1].toInt(),v[2].toDouble(),v[3].toDouble()) } }
    private fun showBishopCalc() { calcInputDialog("Bishop 宫颈评分", listOf("宫口扩张 0-3" to "", "宫颈消退 0-3" to "", "先露高低 0-3" to "", "宫颈硬度 0-2" to "", "宫颈位置 0-2" to "")) { v -> val r = MedicalCalculator.bishop(v[0].toInt(),v[1].toInt(),v[2].toInt(),v[3].toInt(),v[4].toInt()); "${r.score}分 ${r.ripening}\n${r.recommendation}" } }
    private fun showApgarCalc() { calcInputDialog("APGAR", listOf("肤色 0/1/2" to "", "心率 0/1/2" to "", "反应 0/1/2" to "", "肌张力 0/1/2" to "", "呼吸 0/1/2" to "")) { v -> MedicalCalculator.apgar(v[0].toInt(),v[1].toInt(),v[2].toInt(),v[3].toInt(),v[4].toInt()) } }
    private fun showHomaCalc() { calcInputDialog("HOMA-IR", listOf("空腹胰岛素 µU/mL" to "", "空腹血糖 mmol/L" to "")) { v -> MedicalCalculator.homaIr(v[0].toDouble(),v[1].toDouble()) } }
    private fun showCalcCaAg() { calcInputDialog("矫正血钙+AG", listOf("血钙 mmol/L" to "", "白蛋白 g/L" to "", "Na mmol/L" to "", "Cl mmol/L" to "", "HCO₃ mmol/L" to "")) { v -> "矫正Ca: %.2f\n阴离子间隙: %.1f".format(MedicalCalculator.correctedCalcium(v[0].toDouble(),v[1].toDouble()), MedicalCalculator.anionGap(v[2].toDouble(),v[3].toDouble(),v[4].toDouble())) } }
    private fun showSerumOsmCalc() { calcInputDialog("血渗透压", listOf("Na mmol/L" to "", "血糖 mmol/L" to "", "BUN mmol/L" to "")) { v -> "血渗透压: %.0f mOsm/kg".format(MedicalCalculator.serumOsm(v[0].toDouble(),v[1].toDouble(),v[2].toDouble())) } }
    private fun showKDeficitCalc() { calcInputDialog("补钾量", listOf("体重 kg" to "", "目标K" to "", "当前K" to "")) { v -> val d = MedicalCalculator.potassiumDeficit(v[0].toDouble(),v[1].toDouble(),v[2].toDouble()); "缺K: %.0f mmol\n≈ %.0f mL 15%%KCl".format(d, d/2.0) } }
    private fun showSteroidCalc() { val drugs = arrayOf("泼尼松/强的松", "甲泼尼龙", "地塞米松", "氢化可的松"); AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("激素等效换算").setItems(drugs) { _, i -> calcInputDialog("${drugs[i]}等效剂量", listOf("剂量 mg" to "")) { v -> val eq = MedicalCalculator.prednisoneEquivalent(v[0].toDouble(), drugs[i]); "等效泼尼松: %.1f mg".format(eq) } }.setNegativeButton("取消", null).show() }

    // new calculators
    private fun showSofaCalc() { calcInputDialog("SOFA 评分", listOf("呼吸 PaO₂/FiO₂ 0-4" to "", "凝血 血小板 0-4" to "", "肝 胆红素 0-4" to "", "心血管 MAP/升压药 0-4" to "", "CNS GCS 0-4" to "", "肾 肌酐/尿量 0-4" to "")) { v -> val r = MedicalCalculator.sofa(v[0].toInt(),v[1].toInt(),v[2].toInt(),v[3].toInt(),v[4].toInt(),v[5].toInt()); "SOFA=${r.score}分 ${r.mortality}" } }
    private fun showApacheCalc() { calcInputDialog("APACHE II", listOf("体温 ℃" to "", "MAP mmHg" to "", "心率 次/分" to "", "呼吸 次/分" to "", "PaO₂ mmHg" to "", "FiO₂ 0.21-1.0" to "", "pH" to "", "Na mmol/L" to "", "K mmol/L" to "", "肌酐 mg/dL" to "", "HCT %" to "", "WBC 10⁹/L" to "", "GCS 3-15" to "", "年龄" to "", "慢性病 0/1" to "")) { v -> MedicalCalculator.apacheII(v[0].toDouble(),v[1].toInt(),v[2].toInt(),v[3].toInt(),v[4].toDouble(),v[5].toDouble(),v[6].toDouble(),v[7].toDouble(),v[8].toDouble(),v[9].toDouble(),v[10].toDouble(),v[11].toDouble(),v[12].toInt(),v[13].toInt(),v[14]=="1") } }
    private fun showNews2Calc() { calcInputDialog("NEWS2", listOf("呼吸 次/分" to "", "SpO₂ %" to "", "吸氧 0/1" to "", "收缩压 mmHg" to "", "心率 次/分" to "", "体温 ℃" to "", "意识 A=0/V=1/P=2/U=3" to "")) { v -> val r = MedicalCalculator.news2(v[0].toInt(),v[1].toInt(),v[2]=="1",v[3].toInt(),v[4].toInt(),v[5].toDouble(),v[6].toInt()); "NEWS2=${r.score}分 ${r.risk}\n${r.action}" } }

    // new calculators v2
    private fun showRansonCalc() { calcInputDialog("Ranson 胰腺炎", listOf("年龄>55 0/1" to "", "WBC>16 0/1" to "", "血糖>11.1 0/1" to "", "LDH>350 0/1" to "", "AST>250 0/1" to "", "HCT<30 0/1" to "", "BUN>16 0/1" to "", "血钙<2.0 0/1" to "", "PaO₂<60 0/1" to "", "碱缺>-4 0/1" to "", "补液>6L 0/1" to "")) { v -> MedicalCalculator.ranson(v[0].toInt(),v[1].toDouble(),v[2].toDouble(),v[3].toDouble(),v[4].toDouble(),v[5].toDouble(),v[6].toDouble(),v[7].toDouble(),v[8].toDouble(),v[9].toDouble(),v[10].toDouble()) } }
    private fun showCentorCalc() { showCheckboxCalc("Centor 咽炎", listOf("发热>38℃", "扁桃体渗出", "颈淋巴结肿痛", "无咳嗽", "3-15岁")) { v -> MedicalCalculator.centor(v[0]==1,v[1]==1,v[2]==1,v[3]==1,v[4]==1) } }
    private fun showAlvaradoCalc() { calcInputDialog("Alvarado 阑尾炎", listOf("游走痛 1/0" to "", "厌食 1/0" to "", "恶心 1/0" to "", "右下腹压痛 2/0" to "", "反跳痛 1/0" to "", "体温>37.3 1/0" to "", "WBC>10 2/0" to "", "核左移>75% 1/0" to "")) { v -> MedicalCalculator.alvarado(v[0].toInt(),v[1].toInt(),v[2].toInt(),v[3].toInt(),v[4].toInt(),v[5].toInt(),v[6].toInt(),v[7].toInt()) } }
    private fun showGenevaPeCalc() { calcInputDialog("Geneva PE", listOf("年龄>65 0/1" to "", "既往DVT/PE 0/3" to "", "近1月手术 0/2" to "", "活动性肿瘤 0/2" to "", "咯血 0/2" to "", "心率" to "", "单侧下肢痛 0/1" to "")) { v -> MedicalCalculator.genevaPe(v[0].toInt(),v[1]=="3",v[2]=="2",v[3]=="2",v[4]=="2",v[5].toInt(),v[6]=="1") } }
    private fun showPaduaCalc() { showCheckboxCalc("Padua 血栓风险", listOf("活动性肿瘤(+3)", "VTE史(+3)", "活动减少(+3)", "易栓(+3)", "创伤/手术(+2)", "≥70岁", "心衰/呼衰", "心梗/卒中", "感染/风湿", "肥胖BMI≥30", "激素/避孕药")) { v -> MedicalCalculator.padua(v[0]==1,v[1]==1,v[2]==1,v[3]==1,v[4]==1,v[5]==1,v[6]==1,v[7]==1,v[8]==1,v[9]==1,v[10]==1) } }
    // ABG
    private fun showAbgCalc() { calcInputDialog("ABG血气分析", listOf("pH" to "", "PaCO₂ mmHg" to "", "HCO₃⁻ mmol/L" to "", "Na⁺ mmol/L" to "", "Cl⁻ mmol/L" to "", "乳酸(0=跳过)" to "0", "白蛋白 g/L" to "40")) { v -> MedicalCalculator.abgInterpret(v[0].toDouble(),v[1].toDouble(),v[2].toDouble(),v[3].toDouble(),v[4].toDouble(),v[5].toDouble(),v[6].toDouble()) } }

    // ===== 疾病速查 (960种/32科室) =====
    private var diseaseFavorites: MutableSet<String>
        get() = prefs.getStringSet("disease_favorites", emptySet()).toMutableSet()
        set(v) { prefs.putStringSet("disease_favorites", v) }

    private var diseaseRecent: MutableList<String>
        get() = prefs.getString("disease_recent", "").let { j ->
            try { j.removeSurrounding("[", "]").split("||").filter { s -> s.isNotBlank() }.toMutableList() } catch(_: Exception) { mutableListOf() }
        }
        set(v) { prefs.putString("disease_recent", "[" + v.joinToString("||") + "]") }

    private fun showDiseaseLookup() {
        val diseases = com.mynote.android.util.DiseaseReference.getAll()
        val depts = diseases.map { it.department }.distinct()

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 8, 16, 0) }
        // 收藏入口
        if (diseaseFavorites.isNotEmpty()) {
            root.addView(TextView(this).apply {
                text = "⭐ 收藏 (${diseaseFavorites.size})"; textSize = 14f; setTextColor(Color.parseColor("#E65100"))
                setPadding(16, 10, 16, 10); setOnClickListener { showFavorites() }
            })
        }
        if (diseaseRecent.isNotEmpty()) {
            root.addView(TextView(this).apply {
                text = "🕐 最近查看 (${diseaseRecent.size})"; textSize = 13f; setTextColor(Color.parseColor("#1565C0"))
                setPadding(16, 6, 16, 10); setOnClickListener { showDiseaseRecent() }
            })
        }
        // 搜索框
        val et = EditText(this).apply { hint = "输入疾病名搜索…"; inputType = InputType.TYPE_CLASS_TEXT; setPadding(16, 10, 16, 10) }
        root.addView(et)
        root.addView(TextView(this).apply { text = "按科室浏览 (${depts.size}个)"; textSize = 13f; setTypeface(null, Typeface.BOLD); setPadding(16, 8, 0, 4) })

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (d in depts) {
            val count = diseases.count { it.department == d }
            list.addView(TextView(this).apply {
                text = "$d  ($count)"; textSize = 13f; setPadding(32, 10, 32, 10)
                setOnClickListener { showDiseaseDept(diseases.filter { it.department == d }) }
            })
        }
        scroll.addView(list); root.addView(scroll)

        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim().lowercase(); if (q.length < 1) return
                val matched = diseases.filter { it.name.lowercase().contains(q) || it.department.lowercase().contains(q) || it.symptoms.lowercase().contains(q) }
                AlertDialog.Builder(this@SettingsActivity, R.style.RoundedDialog).setTitle("\"$q\" (${matched.size})")
                    .setItems(matched.map { "${it.name} [${it.department}]" }.toTypedArray()) { _, i -> showDiseaseDetail(matched[i]) }
                    .setNegativeButton("返回", null).show()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("疾病速查 · 32科室·960种").setView(root).setNegativeButton("关闭", null).show()
    }

    private fun showFavorites() {
        val diseases = com.mynote.android.util.DiseaseReference.getAll()
        val favs = diseaseFavorites.mapNotNull { name -> diseases.find { it.name == name } }
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("⭐ 收藏疾病")
            .setItems(favs.map { "${it.name} [${it.department}]" }.toTypedArray()) { _, i -> showDiseaseDetail(favs[i]) }
            .setNegativeButton("返回") { _, _ -> showDiseaseLookup() }.show()
    }

    private fun showDiseaseRecent() {
        val diseases = com.mynote.android.util.DiseaseReference.getAll()
        val recents = diseaseRecent.mapNotNull { name -> diseases.find { it.name == name } }
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("🕐 最近查看")
            .setItems(recents.map { "${it.name} [${it.department}]" }.toTypedArray()) { _, i -> showDiseaseDetail(recents[i]) }
            .setNegativeButton("返回") { _, _ -> showDiseaseLookup() }.show()
    }

    private fun showDiseaseDept(list: List<com.mynote.android.util.DiseaseReference.Disease>) {
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(list.firstOrNull()?.department ?: "")
            .setItems(list.map { it.name }.toTypedArray()) { _, i -> showDiseaseDetail(list[i]) }
            .setNegativeButton("返回") { _, _ -> showDiseaseLookup() }.show()
    }

    private fun showDiseaseDetail(d: com.mynote.android.util.DiseaseReference.Disease) {
        // 记录最近查看
        var r = diseaseRecent.toMutableList(); r.remove(d.name); r.add(0, d.name)
        if (r.size > 20) r = r.take(20).toMutableList(); diseaseRecent = r
        val isFav = d.name in diseaseFavorites
        val m = "【症状】${d.symptoms}\n\n【鉴别诊断】${d.differential}\n\n【常用药物】${d.drugs}\n\n【治疗手段】${d.treatment}"
        val full = "${d.name} [${d.department}]\n${m}"
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("${d.name} [${d.department}]${if (isFav) " ⭐" else ""}")
            .setMessage(m)
            .setPositiveButton("📋 复制") { _, _ ->
                (getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager)
                    ?.setPrimaryClip(android.content.ClipData.newPlainText("disease", full))
                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton(if (isFav) "取消收藏" else "⭐ 收藏") { _, _ ->
                val favs = diseaseFavorites
                if (isFav) { favs.remove(d.name); Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show() }
                else { favs.add(d.name); Toast.makeText(this, "已收藏", Toast.LENGTH_SHORT).show() }
                diseaseFavorites = favs
                showDiseaseDetail(d)
            }
            .show()
    }

    // ===== 用药相互作用 (DDInter 30万+对) =====
    private var selectedDrugA: String? = null   // DDInter英文名
    private var selectedDrugB: String? = null
    private var currentDialog: AlertDialog? = null

    private fun showDrugInteraction() {
        selectedDrugA = null; selectedDrugB = null
        showDrugMainDialog()
    }

    private fun showDrugMainDialog() {
        currentDialog?.dismiss()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 24, 40, 8) }

        val tvA = TextView(this).apply {
            text = if (selectedDrugA != null) "✓ ${selectedDrugA}" else "▸ 点击选择药物1"
            textSize = 15f; setTextColor(if (selectedDrugA != null) Color.BLACK else Color.GRAY)
            setPadding(12, 14, 12, 14)
            setBackgroundColor(Color.argb(20, 0, 120, 220))
            setOnClickListener {
                currentDialog?.dismiss()
                showDrugPicker("药物1") { en, _ -> selectedDrugA = en; showDrugMainDialog() }
            }
        }
        root.addView(tvA)

        val tvB = TextView(this).apply {
            text = if (selectedDrugB != null) "✓ ${selectedDrugB}" else "▸ 点击选择药物2"
            textSize = 15f; setTextColor(if (selectedDrugB != null) Color.BLACK else Color.GRAY)
            setPadding(12, 14, 12, 14)
            (layoutParams as? LinearLayout.LayoutParams)?.topMargin = (12 * resources.displayMetrics.density).toInt()
            setBackgroundColor(Color.argb(20, 0, 120, 220))
            setOnClickListener {
                currentDialog?.dismiss()
                showDrugPicker("药物2") { en, _ -> selectedDrugB = en; showDrugMainDialog() }
            }
        }
        root.addView(tvB)

        root.addView(TextView(this).apply {
            text = if (com.mynote.android.util.DrugInteractionEngine.isLoaded()) "DDInter 2.0 · 30万+对" else "内置19条规则"
            textSize = 11f; setTextColor(Color.GRAY); setPadding(0, 12, 0, 0)
        })

        val dlg = AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("用药相互作用检查")
            .setView(root)
            .setPositiveButton("检查") { _, _ ->
                val a = selectedDrugA; val b = selectedDrugB
                if (a == null || b == null) {
                    Toast.makeText(this, "请先选择两种药物", Toast.LENGTH_SHORT).show()
                    showDrugMainDialog()
                    return@setPositiveButton
                }
                AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("相互作用")
                    .setMessage(checkDrugInteraction(a, b)).setPositiveButton("确定", null).show()
            }
            .setNegativeButton("取消") { _, _ -> currentDialog = null }
            .setOnDismissListener { currentDialog = null }
            .create()
        currentDialog = dlg
        dlg.show()
        dlg.window?.setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    /** 药品搜索选择器 */
    private fun showDrugPicker(title: String, onSelected: (enName: String, zhLabel: String?) -> Unit) {
        val engine = com.mynote.android.util.DrugInteractionEngine
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 0) }
        val editText = EditText(this).apply {
            hint = "输入药名（中文或英文）"
            setSingleLine(true); textSize = 15f
        }
        root.addView(editText)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (400 * resources.displayMetrics.density).toInt()
            )
        }
        val listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollView.addView(listLayout)
        root.addView(scrollView)

        var pickerDialog: AlertDialog? = null

        fun refreshList() {
            val query = editText.text.toString().trim()
            listLayout.removeAllViews()
            val results = engine.searchDrugNames(query, limit = 40)

            if (results.isEmpty() && query.isNotEmpty()) {
                listLayout.addView(TextView(this@SettingsActivity).apply {
                    text = "未匹配到药品\n可尝试英文通用名搜索"
                    textSize = 13f; setTextColor(Color.GRAY); gravity = android.view.Gravity.CENTER
                    setPadding(0, 60, 0, 0)
                })
            }

            for (match in results) {
                val item = TextView(this@SettingsActivity).apply {
                    val display = if (match.zhLabel != null) "${match.zhLabel}  (${match.enName})" else match.enName
                    text = display
                    textSize = 14f; setTextColor(Color.BLACK)
                    setPadding(12, 14, 12, 14)
                    setOnClickListener {
                        onSelected(match.enName, match.zhLabel)
                        pickerDialog?.dismiss()
                    }
                }
                if (listLayout.childCount > 0) {
                    val sep = View(this@SettingsActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                        setBackgroundColor(Color.argb(30, 0, 0, 0))
                    }
                    listLayout.addView(sep)
                }
                listLayout.addView(item)
            }
        }

        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { refreshList() }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        refreshList()

        val dlg = AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle(title)
            .setView(root)
            .setNegativeButton("取消") { _, _ -> showDrugMainDialog() }
            .setOnDismissListener { if (pickerDialog === it) showDrugMainDialog() }
            .create()
        pickerDialog = dlg
        dlg.show()
        dlg.window?.setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun checkDrugInteraction(a: String, b: String): String {
        val engine = com.mynote.android.util.DrugInteractionEngine
        if (engine.isLoaded()) {
            val allMatches = engine.check(a, b)
            // 过滤：Level 1 始终保留（硬禁忌有意义），Level 2/3 必须有描述
            val matches = allMatches.filter { it.lvl == 1 || it.desc.isNotBlank() || it.mng.isNotBlank() }
            if (matches.isNotEmpty()) {
                return buildString {
                    for ((i, m) in matches.take(5).withIndex()) {
                        if (i > 0) append("\n\n───────────\n\n")
                        append("${m.a} × ${m.b}\n")
                        append(engine.levelLabel(m.lvl))
                        if (m.desc.isNotBlank()) append("\n${m.desc}")
                        if (m.mng.isNotBlank()) append("\n处理: ${m.mng}")
                        if (m.lvl == 1 && m.desc.isBlank() && m.mng.isBlank()) {
                            append("\n(DDInter 标记为禁忌/不推荐，未提供具体描述)")
                        }
                    }
                    if (matches.size > 5) append("\n\n...共${matches.size}条，仅显示前5条")
                    if (allMatches.size > matches.size) {
                        append("\n\n(已过滤 ${allMatches.size - matches.size} 条缺乏描述的 Level 2/3 结果)")
                    }
                }
            }
            // DDInter 所有结果被过滤或无数据 → 尝试类级别规则
            val classHint = checkClassInteraction(a, b)
            if (classHint != null) return classHint
            if (allMatches.isNotEmpty()) {
                return "DDInter 收录「$a」与「$b」仅标记了严重等级（Level 2/3），缺乏具体描述，临床意义不确定。"
            }
            return "DDInter 未收录「$a」与「$b」的直接相互作用（不代表绝对安全）"
        }
        // 无 DDInter 数据时退回硬编码规则 → 再试类级别
        val manual = checkManualRules(a, b)
        if (manual != null) return manual
        val classHint = checkClassInteraction(a, b)
        if (classHint != null) return classHint
        return "未发现已知严重相互作用\n(仅覆盖常见高危组合，临床决策仍需综合判断)"
    }

    // 19条高危手写规则
    private fun checkManualRules(a: String, b: String): String? {
        data class Rule(val d1: String, val d2: String, val result: String)
        val rules = listOf(
            Rule("阿司匹林","布洛芬","⚠️ 布洛芬减弱阿司匹林抗血小板+协同GI损伤"),
            Rule("阿司匹林","氯吡格雷","⚠️ 出血风险↑ ACS联用获益>风险"),
            Rule("氯吡格雷","奥美拉唑","⚠️ 奥美拉唑减弱氯吡格雷→血栓风险 改用泮托拉唑"),
            Rule("华法林","阿司匹林","❌ 禁忌！严重出血(颅内/消化道)"),
            Rule("华法林","甲硝唑","❌ 甲硝唑→INR飙升 华法林减量1/3-1/2"),
            Rule("华法林","胺碘酮","❌ 胺碘酮→INR↑50-100% 华法林减量30-50%"),
            Rule("ACEI","螺内酯","⚠️ 高钾血症风险↑ 严密监测血钾"),
            Rule("ACEI","NSAIDs","⚠️ NSAIDs减弱降压+肾损伤风险↑"),
            Rule("地高辛","胺碘酮","❌ 地高辛浓度↑50-100% 减半量"),
            Rule("辛伐他汀","胺碘酮","❌ 横纹肌溶解 辛伐他汀≤20mg/d"),
            Rule("他汀","克拉霉素","❌ 横纹肌溶解 暂停他汀"),
            Rule("华法林","利福平","⚠️ 利福平↓华法林效价 需增量"),
            Rule("二甲双胍","造影剂","⚠️ 乳酸酸中毒 造影前后停48h"),
            Rule("呋塞米","庆大霉素","❌ 耳毒性+肾毒性叠加"),
            Rule("SSRI","MAOI","❌ 5-HT综合征 致死性"),
            Rule("甲氨蝶呤","NSAIDs","❌ MTX清除↓→骨髓抑制"),
            Rule("锂盐","NSAIDs","❌ 锂排泄↓→锂中毒"),
            Rule("ACEI","补钾","❌ 高危高钾 禁止常规联用"),
            Rule("他汀","夫西地酸","⚠️ 横纹肌溶解风险 暂停他汀"),
        )
        val al = a.lowercase(); val bl = b.lowercase()
        for (r in rules) {
            if ((al.contains(r.d1.lowercase()) && bl.contains(r.d2.lowercase())) ||
                (al.contains(r.d2.lowercase()) && bl.contains(r.d1.lowercase())))
                return "${r.d1} × ${r.d2}\n${r.result}"
        }
        return null
    }

    /** 类级别相互作用 — DDInter和手写规则都未命中时兜底 */
    private fun checkClassInteraction(a: String, b: String): String? {
        data class ClassRule(val pA: List<String>, val pB: List<String>, val label: String, val guidance: String)
        val rules = listOf(
            // == NSAID 相关 ==
            ClassRule(listOf("ibuprofen","diclofenac","naproxen","celecoxib","indomethacin","ketorolac","meloxicam","etoricoxib","piroxicam"),
                listOf("ibuprofen","diclofenac","naproxen","celecoxib","indomethacin","ketorolac","meloxicam","etoricoxib","piroxicam","aspirin","acetylsalicylic"),
                "⚠️ NSAID + NSAID",
                "不建议两种NSAIDs联合使用。不增加镇痛效果，但显著增加胃肠道出血、穿孔及急性肾损伤风险。如确需联合，加用PPI保护胃黏膜。"),
            ClassRule(listOf("ibuprofen","diclofenac","naproxen","celecoxib","indomethacin","ketorolac","meloxicam"),
                listOf("warfarin","rivaroxaban","apixaban","dabigatran","edoxaban","heparin","enoxaparin","clopidogrel","ticagrelor"),
                "⚠️ NSAID + 抗凝/抗血小板",
                "出血风险叠加增加。NSAIDs抑制血小板功能+损伤胃黏膜，与抗凝药联用显著增加GI出血风险。建议加用PPI，监测出血征象。"),
            ClassRule(listOf("ibuprofen","diclofenac","naproxen","celecoxib","indomethacin","ketorolac","meloxicam"),
                listOf("dexamethasone","prednisone","prednisolone","methylprednisolone","hydrocortisone","cortisone"),
                "⚠️ NSAID + 糖皮质激素",
                "协同增加上消化道溃疡、出血、穿孔风险。如联用，常规加用PPI保护胃黏膜。"),
            ClassRule(listOf("ibuprofen","diclofenac","naproxen","celecoxib","indomethacin","ketorolac","meloxicam"),
                listOf("losartan","valsartan","irbesartan","telmisartan","candesartan","olmesartan",
                       "captopril","enalapril","lisinopril","ramipril","perindopril","benazepril","fosinopril","trandolapril","quinapril"),
                "⚠️ NSAID + ACEI/ARB",
                "NSAIDs可减弱ACEI/ARB降压效果，并增加急性肾损伤风险（尤其容量不足、心衰、CKD患者）。监测血压和肾功能。"),

            // == 降压药联合 ==
            ClassRule(listOf("losartan","valsartan","irbesartan","telmisartan","candesartan","olmesartan"),
                listOf("bisoprolol","metoprolol","atenolol","carvedilol","propranolol","nebivolol","labetalol"),
                "ARB + β受体阻滞剂",
                "临床常用联合方案。用于高血压、心衰、心梗后患者。注意监测血压和心率，避免心动过缓。"),
            ClassRule(listOf("captopril","enalapril","lisinopril","ramipril","perindopril","benazepril","fosinopril"),
                listOf("bisoprolol","metoprolol","atenolol","carvedilol","propranolol","nebivolol","labetalol"),
                "ACEI + β受体阻滞剂",
                "心衰标准治疗组合(GDMT)。协同改善预后。监测血压、心率、肾功能和血钾。"),
            ClassRule(listOf("losartan","valsartan","irbesartan","telmisartan","candesartan","olmesartan"),
                listOf("amlodipine","nifedipine","felodipine","nitrendipine","lercanidipine"),
                "ARB + 二氢吡啶CCB",
                "指南推荐的一线联合降压方案，多国已有SPC复方制剂。安全有效，不良反应少。"),
            ClassRule(listOf("captopril","enalapril","lisinopril","ramipril","perindopril","benazepril"),
                listOf("amlodipine","nifedipine","felodipine","nitrendipine","lercanidipine"),
                "ACEI + 二氢吡啶CCB",
                "常用联合降压方案。耐受性好，SPC复方可提高依从性。"),
            ClassRule(listOf("losartan","valsartan","irbesartan","telmisartan","candesartan","olmesartan",
                       "captopril","enalapril","lisinopril","ramipril","perindopril","benazepril"),
                listOf("hydrochlorothiazide","chlorthalidone","indapamide"),
                "ACEI/ARB + 噻嗪类利尿剂",
                "经典联合降压组合，指南推荐。补钾效应部分抵消。监测血压、电解质和肾功能。"),
            ClassRule(listOf("losartan","valsartan","irbesartan","telmisartan","candesartan","olmesartan",
                       "captopril","enalapril","lisinopril","ramipril","perindopril","benazepril"),
                listOf("spironolactone","eplerenone","finerenone","amiloride","triamterene"),
                "⚠️ ACEI/ARB + 保钾利尿剂",
                "高钾血症风险增加，尤其CKD、糖尿病、老年患者。需定期监测血钾。心衰中螺内酯+ACEI为GDMT但需严密监测。"),
            ClassRule(listOf("bisoprolol","metoprolol","atenolol","carvedilol","propranolol","nebivolol"),
                listOf("verapamil","diltiazem"),
                "⚠️ β受体阻滞剂 + 非二氢吡啶CCB",
                "叠加负性肌力/负性频率作用→严重心动过缓、房室传导阻滞、心衰加重、心脏骤停风险。通常避免联合。"),
            ClassRule(listOf("bisoprolol","metoprolol","atenolol","carvedilol"),
                listOf("amlodipine","nifedipine","felodipine","nitrendipine","lercanidipine"),
                "β受体阻滞剂 + 二氢吡啶CCB",
                "常用联合降压方案。CCB反射性交感激活可被BB拮抗。注意监测心率和血压。"),

            // == 他汀 ==
            ClassRule(listOf("atorvastatin","rosuvastatin","simvastatin","pravastatin","fluvastatin","pitavastatin"),
                listOf("gemfibrozil","fenofibrate","bezafibrate"),
                "⚠️ 他汀 + 贝特类",
                "横纹肌溶解风险增加（尤其吉非罗齐）。如确需联用，首选非诺贝特+低剂量他汀，监测CK和肝肾功能。"),
            ClassRule(listOf("simvastatin","atorvastatin","rosuvastatin"),
                listOf("amiodarone"),
                "⚠️ 他汀 + 胺碘酮",
                "胺碘酮抑制CYP3A4→他汀浓度↑→横纹肌溶解风险。辛伐他汀限制≤20mg/d，阿托伐他汀≤40mg/d。瑞舒伐他汀不依赖CYP3A4代谢，风险较低。"),

            // == 糖尿病 == 
            ClassRule(listOf("metformin"),
                listOf("insulin","insulin glargine","insulin aspart","insulin detemir","insulin lispro","insulin degludec"),
                "二甲双胍 + 胰岛素",
                "常用联合方案。二甲双胍改善胰岛素抵抗，可减少胰岛素用量。注意监测血糖防低血糖。"),
            ClassRule(listOf("metformin"),
                listOf("dapagliflozin","empagliflozin","canagliflozin","ertugliflozin"),
                "二甲双胍 + SGLT2i",
                "ADA/EASD指南一线联合推荐。心血管和肾脏获益明确，低血糖风险低。"),
            ClassRule(listOf("metformin"),
                listOf("sitagliptin","saxagliptin","linagliptin","vildagliptin","alogliptin"),
                "二甲双胍 + DPP-4i",
                "常用口服降糖联合方案，低血糖风险低，体重中性。"),
            ClassRule(listOf("dapagliflozin","empagliflozin","canagliflozin"),
                listOf("insulin","insulin glargine","insulin aspart","insulin detemir","insulin lispro","insulin degludec"),
                "SGLT2i + 胰岛素",
                "联合可改善血糖控制+减重+心血管保护。启用SGLT2i后胰岛素需减量10-20%防低血糖。"),
            ClassRule(listOf("liraglutide","semaglutide","dulaglutide","exenatide","tirzepatide"),
                listOf("insulin","insulin glargine","insulin aspart","insulin detemir"),
                "GLP-1RA + 胰岛素",
                "常用联合。启用GLP-1RA后胰岛素需减量防低血糖。减重获益+心血管保护。"),

            // == 神经精神 ==
            ClassRule(listOf("morphine","fentanyl","oxycodone","hydromorphone","tramadol","codeine"),
                listOf("diazepam","midazolam","lorazepam","alprazolam","clonazepam","temazepam","oxazepam"),
                "⚠️ 阿片类 + 苯二氮卓类",
                "协同CNS抑制→过度镇静、呼吸抑制、昏迷、死亡。FDA黑框警告。如联用，用最低有效剂量最短时间。"),
            ClassRule(listOf("sertraline","escitalopram","fluoxetine","paroxetine","citalopram","fluvoxamine"),
                listOf("venlafaxine","duloxetine","amitriptyline","nortriptyline","imipramine","clomipramine","desipramine"),
                "⚠️ SSRI + SNRI/TCA",
                "5-HT综合征风险。SSRI+TCA还因CYP2D6抑制→TCA浓度↑。如联用从低剂量开始，监测5-HT综合征。"),
            ClassRule(listOf("sertraline","escitalopram","fluoxetine","paroxetine","citalopram"),
                listOf("clopidogrel","aspirin","warfarin","rivaroxaban","apixaban"),
                "⚠️ SSRI + 抗血小板/抗凝",
                "SSRI抑制血小板5-HT再摄取→出血风险↑。联合使用GI出血风险约增1.5-2倍。建议加用PPI或选非SSRI类抗抑郁药。"),
            ClassRule(listOf("lithium","lithium carbonate"),
                listOf("ibuprofen","diclofenac","naproxen","celecoxib","indomethacin","ketorolac","meloxicam"),
                "⚠️ 锂盐 + NSAID",
                "NSAIDs减少肾脏锂排泄→血锂浓度升高→锂中毒风险。避免联合，如必须使用选对锂影响较小的ASA或sulindac。"),
            ClassRule(listOf("lithium","lithium carbonate"),
                listOf("hydrochlorothiazide","furosemide","spironolactone"),
                "⚠️ 锂盐 + 利尿剂",
                "利尿剂改变锂排泄→血锂浓度波动。噻嗪类减少锂清除增血锂，襻利尿剂影响较小但需监测。定期查血锂浓度。"),

            // == 心衰/心律失常 ==
            ClassRule(listOf("digoxin"),
                listOf("amiodarone","verapamil","diltiazem"),
                "⚠️ 地高辛 + 胺碘酮/非二氢吡啶CCB",
                "显著增加地高辛血药浓度（胺碘酮↑50-100%，维拉帕米↑50-75%）→洋地黄中毒风险。地高辛减量50%并监测浓度。"),
            ClassRule(listOf("digoxin"),
                listOf("bisoprolol","metoprolol","atenolol","carvedilol"),
                "地高辛 + β受体阻滞剂",
                "房颤/心衰常用联合。协同控制心室率，但需注意叠加心动过缓/房室传导阻滞风险。监测心率和ECG。"),

            // == 其他常见组合 ==
            ClassRule(listOf("levothyroxine","thyroxine"),
                listOf("calcium carbonate","ferrous sulfate","ferrous fumarate","ferrous gluconate"),
                "⚠️ 左甲状腺素 + 钙/铁剂",
                "钙/铁剂减少左甲状腺素吸收。间隔至少4小时服用。建议甲状腺素空腹服用，钙铁剂随餐或睡前。"),
            ClassRule(listOf("clarithromycin","erythromycin"),
                listOf("atorvastatin","simvastatin","rosuvastatin"),
                "⚠️ 大环内酯类(CYP3A4抑制) + 他汀",
                "克拉霉素/红霉素抑制CYP3A4→他汀浓度↑→横纹肌溶解。辛伐他汀/阿托伐他汀暂停或减量。阿奇霉素对CYP3A4影响小可替代。"),
            ClassRule(listOf("levofloxacin","ciprofloxacin","moxifloxacin","ofloxacin"),
                listOf("sucralfate","calcium carbonate","ferrous sulfate","magnesium hydroxide","aluminum hydroxide"),
                "⚠️ 喹诺酮类 + 多价阳离子",
                "钙、铁、镁、铝等与喹诺酮螯合→吸收↓→治疗失败。间隔至少2-4小时服用。"),
            ClassRule(listOf("propranolol","carvedilol","labetalol"),
                listOf("insulin","glimepiride","glyburide","glipizide","repaglinide","nateglinide"),
                "⚠️ 非选择性β阻滞剂 + 胰岛素/磺脲类",
                "非选择性β阻滞剂可掩盖低血糖症状(心悸、震颤)和延迟恢复。选用选择性β1阻滞剂（美托洛尔/比索洛尔）可降低此风险。"),
        )

        val al = a.lowercase(); val bl = b.lowercase()
        for (rule in rules) {
            val aMatch = rule.pA.any { al == it || al.contains(it) || it.contains(al) }
            val bMatch = rule.pB.any { bl == it || bl.contains(it) || it.contains(bl) }
            val bMatchA = rule.pA.any { bl == it || bl.contains(it) || it.contains(bl) }
            val aMatchB = rule.pB.any { al == it || al.contains(it) || it.contains(al) }
            if ((aMatch && bMatch) || (bMatchA && aMatchB)) {
                return "${rule.label}\n${rule.guidance}"
            }
        }
        return null
    }

    // ===== 指南速查 =====
    private fun showGuideLookup() {
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("指南速查")
            .setItems(arrayOf("按科室查看", "按年份查看 (2024-2026)")) { _, mode ->
                if (mode == 0) showGuideByDept() else showGuideByYear()
            }.setNegativeButton("关闭", null).show()
    }

    private fun showGuideByDept() {
        val diseases = com.mynote.android.util.DiseaseReference.getAll()
        val depts = diseases.map { it.department }.distinct()
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("指南速查 · 按科室")
            .setItems(depts.map { d -> "${d} (${diseases.count { it.department == d }})" }.toTypedArray()) { _, di ->
                val deptDiseases = diseases.filter { it.department == depts[di] }
                AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("${depts[di]} (${deptDiseases.size})")
                    .setItems(deptDiseases.map { it.name }.toTypedArray()) { _, i ->
                        val d = deptDiseases[i]
                        val years = mutableListOf<String>()
                        if (d.treatment.contains("202")) years.addAll(listOf("2024", "2025", "2026").filter { d.treatment.contains(it) || d.drugs.contains(it) })
                        val msg = "${if (years.isNotEmpty()) "【指南 (${years.joinToString("/")})】\n" else ""}${d.drugs}\n\n【治疗】${d.treatment.take(200)}\n\n【症状】${d.symptoms.take(150)}\n【鉴别】${d.differential.take(150)}"
                        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("${d.name} ${if (years.isNotEmpty()) "· ${years.joinToString("/")}" else ""}")
                            .setMessage(msg)
                            .setPositiveButton("关闭", null)
                            .setNeutralButton("📋 复制") { _, _ ->
                                (getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager)
                                    ?.setPrimaryClip(android.content.ClipData.newPlainText("disease", "${d.name}\n$msg"))
                                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                            }.show()
                    }.setNegativeButton("返回") { _, _ -> showGuideByDept() }.show()
            }.setNegativeButton("返回") { _, _ -> showGuideLookup() }.show()
    }

    private fun showGuideByYear() {
        val diseases = com.mynote.android.util.DiseaseReference.getAll()
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("指南速查 · 按年份")
            .setItems(arrayOf("2026", "2025", "2024")) { _, yi ->
                val y = if (yi == 0) "2026" else if (yi == 1) "2025" else "2024"
                val m = diseases.filter { it.treatment.contains(y) || it.drugs.contains(y) }
                val depts = m.map { it.department }.distinct()
                AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("${y}年 (${m.size}疾病)")
                    .setItems(depts.map { "$it (${m.count { d -> d.department == it }})" }.toTypedArray()) { _, di ->
                        val list = m.filter { it.department == depts[di] }
                        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("${depts[di]} · $y")
                            .setItems(list.map { it.name }.toTypedArray()) { _, i ->
                                val d = list[i]
                                val msg2 = "【${y}指南用药】${d.drugs}\n\n【${y}指南治疗】${d.treatment}\n\n【症状】${d.symptoms}\n【鉴别】${d.differential}"
                                AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(d.name)
                                    .setMessage(msg2)
                                    .setPositiveButton("关闭", null)
                                    .setNeutralButton("📋 复制") { _, _ ->
                                        (getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager)
                                            ?.setPrimaryClip(android.content.ClipData.newPlainText("disease", "${d.name}\n$msg2"))
                                        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                                    }.show()
                            }.setNegativeButton("返回", null).show()
                    }.setNegativeButton("返回", null).show()
            }.setNegativeButton("返回") { _, _ -> showGuideLookup() }.show()
    }

    private fun showClinicalPathways() {
        val paths = com.mynote.android.util.ClinicalPathways.pathways
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 12, 20, 0) }
        val et = EditText(this).apply { hint = "搜索路径 (如: 胸痛、DKA、卒中...)"; setSingleLine(true); textSize = 14f }
        root.addView(et)
        val tv = TextView(this).apply { text = "共${paths.size}条临床路径"; textSize = 11f; setTextColor(Color.GRAY); setPadding(0, 8, 0, 0) }
        root.addView(tv)

        var dlg: AlertDialog? = null
        fun buildDialog(items: List<String>) {
            dlg?.dismiss()
            dlg = AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("临床路径 · 决策流程")
                .setView(root)
                .setItems(items.toTypedArray()) { _, i2 ->
                    val idx = paths.indexOfFirst { it.title == items[i2] }
                    if (idx >= 0) showPathwayNode(paths[idx])
                }
                .setNegativeButton("关闭", null)
                .setNeutralButton("📤 导出手册") { _, _ -> exportClinicalHandbook() }
                .create()
            dlg!!.show()
            dlg!!.window?.setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        buildDialog(paths.map { it.title })

        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim().lowercase()
                val filtered = if (q.isEmpty()) {
                    tv.text = "共${paths.size}条临床路径"
                    paths.map { it.title }
                } else {
                    fun searchNode(n: com.mynote.android.util.ClinicalPathways.Node): Boolean =
                        n.title.lowercase().contains(q) || n.content.lowercase().contains(q) || n.children.any { searchNode(it) }
                    val matched = paths.filter { searchNode(it) }
                    tv.text = if (matched.isNotEmpty()) "找到${matched.size}条 (共${paths.size}条)" else "未找到，显示全部"
                    matched.ifEmpty { paths }.map { it.title }
                }
                buildDialog(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun exportClinicalHandbook() {
        val paths = com.mynote.android.util.ClinicalPathways.pathways
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
        sb.append("<title>临床参考手册</title>")
        sb.append("<style>body{font-family:sans-serif;max-width:800px;margin:auto;padding:20px;color:#222}")
        sb.append("h1{color:#1a5276;border-bottom:2px solid #2980b9;padding-bottom:8px}")
        sb.append("h2{color:#2c3e50;margin-top:24px}h3{color:#555;margin:8px 0}pre{background:#f0f4f8;padding:12px;border-radius:8px;white-space:pre-wrap}")
        sb.append("</style></head><body><h1>📋 临床参考手册</h1><p>MyNote Android · ${java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}</p>")
        for (p in paths) {
            sb.append("<h2>${p.title}</h2>")
            for (n in p.children) {
                sb.append("<h3>${n.title}</h3><pre>${n.content}</pre>")
            }
        }
        sb.append("</body></html>")
        try {
            val dir = java.io.File(filesDir, "export")
            dir.mkdirs()
            val f = java.io.File(dir, "临床参考手册_${java.text.SimpleDateFormat("MMdd_HHmm", Locale.getDefault()).format(Date())}.html")
            f.writeText(sb.toString(), Charsets.UTF_8)
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", f)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/html"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            startActivity(Intent.createChooser(intent, "导出临床参考手册"))
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPathwayNode(node: com.mynote.android.util.ClinicalPathways.Node) {
        if (node.children.isEmpty()) {
            val text = node.content.ifBlank { "无详细内容" }
            AlertDialog.Builder(this, R.style.RoundedDialog)
                .setTitle(node.title).setMessage(text)
                .setPositiveButton("📋 复制") { _, _ ->
                    (getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager)
                        ?.setPrimaryClip(android.content.ClipData.newPlainText("pathway", "${node.title}\n${text}"))
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("关闭", null).show()
            return
        }
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle(node.title)
            .setItems(node.children.map { it.title }.toTypedArray()) { _, i ->
                showPathwayNode(node.children[i])
            }
            .setNeutralButton("📊 流程图") { _, _ -> showPathwayFlowchart(node) }
            .setNegativeButton("返回", null)
            .show()
    }

    private fun showPathwayFlowchart(node: com.mynote.android.util.ClinicalPathways.Node) {
        val sb = StringBuilder()
        sb.append("<html><head><meta charset='UTF-8'><style>")
        sb.append("body{margin:0;padding:16px;font-family:sans-serif;background:#fff}")
        sb.append(".title{font-size:18px;font-weight:bold;color:#1a5276;margin-bottom:12px}")
        sb.append(".box{padding:10px 14px;margin:6px 0;border-radius:8px;font-size:13px;line-height:1.5}")
        sb.append(".step{background:#e8f0fe;border-left:4px solid #2980b9}")
        sb.append(".decision{background:#fef9e7;border-left:4px solid #e67e22}")
        sb.append(".action{background:#eafaf1;border-left:4px solid #27ae60}")
        sb.append(".sep{text-align:center;color:#888;font-size:20px;margin:2px 0}")
        sb.append("</style></head><body>")
        sb.append("<div class='title'>⬇ ${node.title}</div>")
        for ((i, n) in node.children.withIndex()) {
            if (i > 0) sb.append("<div class='sep'>↓</div>")
            val cls = when {
                n.title.contains("?") || n.title.contains("?") || n.title.contains("评估") || n.title.contains("判断") -> "decision"
                n.title.contains("处理") || n.title.contains("药物") || n.title.contains("抗生素") || n.title.contains("胰岛素") || n.title.contains("再灌注") -> "action"
                else -> "step"
            }
            sb.append("<div class='box $cls'><b>${n.title}</b><br>${n.content.replace("\n", "<br>")}</div>")
        }
        sb.append("</body></html>")
        val wv = WebView(this).apply {
            settings.defaultTextEncodingName = "UTF-8"
            webViewClient = WebViewClient()
            loadDataWithBaseURL(null, sb.toString(), "text/html", "UTF-8", null)
        }
        val dlg = AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("${node.title} · 流程图")
            .setView(wv).setPositiveButton("关闭", null).create()
        dlg.show()
        dlg.window?.setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(), (resources.displayMetrics.heightPixels * 0.75).toInt())
    }

    private fun showLabReference() {
        val labs = com.mynote.android.util.LabReference.all
        val cats = com.mynote.android.util.LabReference.categories
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 8, 16, 0) }
        val et = EditText(this).apply { hint = "搜索检验项目 (如: 肌酐、D-Dimer...)"; setSingleLine(true); textSize = 14f }
        root.addView(et)
        val tv = TextView(this).apply { text = "共${labs.size}项 · ${cats.size}个分类"; textSize = 11f; setTextColor(Color.GRAY); setPadding(0, 6, 0, 4) }
        root.addView(tv)

        fun showList(items: List<com.mynote.android.util.LabReference.LabItem>) {
            AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("检验参考值").setView(root)
                .setItems(items.map { "${it.name} (${it.normalRange} ${it.unit})" }.toTypedArray()) { _, i ->
                    val item = items[i]
                    val info = "[${item.category}] ${item.name}\n\n正常值: ${item.normalRange} ${item.unit}\n\n危急值: ${if (item.criticalLow != "-") "低 <${item.criticalLow} ${item.unit}" else "—"}  ${if (item.criticalHigh != "-") "高 >${item.criticalHigh} ${item.unit}" else "—"}\n\n临床意义: ${item.significance}"
                    AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(item.name).setMessage(info)
                        .setPositiveButton("📋 复制") { _, _ ->
                            (getSystemService(android.content.ClipboardManager::class.java))?.setPrimaryClip(android.content.ClipData.newPlainText("lab", info))
                            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("关闭", null).show()
                }
                .setNegativeButton("关闭", null).show()
        }

        showList(labs)

        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim()
                val filtered = if (q.isEmpty()) labs else com.mynote.android.util.LabReference.search(q)
                tv.text = if (q.isEmpty()) "共${labs.size}项 · ${cats.size}个分类" else "找到${filtered.size}项"
                showList(filtered.ifEmpty { labs })
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    // ===== 影像图鉴 =====
    private fun showImagingReference() {
        val all = com.mynote.android.util.ImagingReference.all
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 8, 16, 0) }
        val et = EditText(this).apply { hint = "搜索影像征象 (如: 骨折、甲状腺...)"; setSingleLine(true); textSize = 14f }
        root.addView(et)
        val tv = TextView(this).apply { text = "共${all.size}条"; textSize = 11f; setTextColor(Color.GRAY); setPadding(0, 6, 0, 4) }
        root.addView(tv)
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listContainer)
        root.addView(scroll)

        fun showList(filtered: List<com.mynote.android.util.ImagingReference.Imaging>) {
            listContainer.removeAllViews()
            val grouped = filtered.groupBy { it.system }
            for ((dept, items) in grouped) {
                // 科室标题
                val header = TextView(this).apply {
                    text = "━━ ${dept} (${items.size}) ━━"
                    textSize = 12f; setTextColor(Color.parseColor("#1565C0"))
                    setPadding(4, 12, 4, 4); typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                listContainer.addView(header)
                for (item in items) {
                    val tvItem = TextView(this).apply {
                        text = "  [${item.modality}] ${item.title}"
                        textSize = 14f; setPadding(4, 8, 4, 8)
                        setTextColor(Color.DKGRAY)
                        setOnClickListener {
                            val url = item.imageUrl.ifBlank { "https://radiopaedia.org/search?q=${item.title.replace(" ", "+")}" }
                            AlertDialog.Builder(this@SettingsActivity, R.style.RoundedDialog).setTitle(item.title)
                                .setMessage("【方式】${item.modality}\n【系统】${item.system}\n\n${item.description}")
                                .setPositiveButton("🔗 在线图例") { _, _ ->
                                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                    catch(e: Exception) { Toast.makeText(this@SettingsActivity, "无法打开链接", Toast.LENGTH_SHORT).show() }
                                }.setNeutralButton("📋 复制") { _, _ ->
                                    (getSystemService(android.content.ClipboardManager::class.java))?.setPrimaryClip(android.content.ClipData.newPlainText("imaging", "${item.title}\n${item.description}"))
                                    Toast.makeText(this@SettingsActivity, "已复制", Toast.LENGTH_SHORT).show()
                                }.setNegativeButton("关闭", null).show()
                        }
                    }
                    listContainer.addView(tvItem)
                }
            }
        }
        showList(all)

        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim()
                val filtered = if (q.isEmpty()) all else com.mynote.android.util.ImagingReference.search(q)
                tv.text = if (q.isEmpty()) "共${all.size}条" else "找到${filtered.size}条"
                showList(filtered.ifEmpty { all })
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        val dlg = AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("影像征象速查").setView(root)
            .setNegativeButton("关闭", null).create()
        dlg.show()
        dlg.window?.setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(), (resources.displayMetrics.heightPixels * 0.75).toInt())
    }

    // ===== AI 读化验单 =====
    private var aiLabImageUri: Uri? = null
    private val aiLabLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { recognizeLabReport(it) }
    }

    private fun startAiLabReader() {
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("AI 读化验单")
            .setMessage("选择化验单图片，AI 自动分析异常指标并给出建议。")
            .setPositiveButton("🖼 选择图片") { _, _ -> aiLabLauncher.launch("image/*") }
            .setNegativeButton("取消", null).show()
    }

    private fun recognizeLabReport(uri: Uri) {
        val tv = TextView(this).apply { text = "⏳ AI 正在分析化验单..."; textSize = 14f; setPadding(32, 24, 32, 24); gravity = android.view.Gravity.CENTER }
        val dlg = AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("AI 分析中").setView(tv).setNegativeButton("关闭", null).show()

        Thread {
            try {
                val bitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val ocrText = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0).let { img ->
                    val result = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions.Builder().build())
                        .process(img).let { com.google.android.gms.tasks.Tasks.await(it) }
                    result.text
                }
                val apiKey = prefs.deepseekApiKey.ifBlank { prefs.qwenApiKey }
                if (apiKey.isBlank()) throw Exception("请先配置 AI API Key")

                val model = if (prefs.deepseekApiKey.isNotBlank()) "deepseek-reasoner" else "qwen-max"
                val endpoint = if (model.startsWith("deepseek")) "https://api.deepseek.com/v1/chat/completions" else "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

                val client = okhttp3.OkHttpClient.Builder().connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS).readTimeout(90, java.util.concurrent.TimeUnit.SECONDS).build()
                val json = com.google.gson.Gson().toJson(mapOf(
                    "model" to model,
                    "messages" to listOf(
                        mapOf("role" to "system", "content" to "你是检验科医生。分析以下化验单OCR文本，列出所有异常指标并标注高低，给出临床意义和建议。简洁专业。"),
                        mapOf("role" to "user", "content" to ocrText)
                    )
                ))
                val mt = "application/json".toMediaType()
                val req = okhttp3.Request.Builder().url(endpoint).header("Authorization", "Bearer $apiKey").post(okhttp3.RequestBody.create(mt, json)).build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                val reply = com.google.gson.JsonParser.parseString(body).asJsonObject?.getAsJsonArray("choices")?.get(0)?.asJsonObject?.getAsJsonObject("message")?.get("content")?.asString ?: ""

                runOnUiThread {
                    dlg.dismiss()
                    AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("AI 分析结果")
                        .setMessage("【OCR原文】\n${ocrText.take(300)}...\n\n【AI分析】\n$reply")
                        .setPositiveButton("📋 复制全部") { _, _ ->
                            (getSystemService(android.content.ClipboardManager::class.java))?.setPrimaryClip(android.content.ClipData.newPlainText("lab", "$ocrText\n\n--- AI 分析 ---\n$reply"))
                            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                        }.setNegativeButton("关闭", null).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    dlg.dismiss()
                    AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("失败").setMessage("${e.message}").setPositiveButton("确定", null).show()
                }
            }
        }.start()
    }

    private fun showEcgReference() {
        val all = com.mynote.android.util.ECGReference.all
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 8, 16, 0) }
        val et = EditText(this).apply { hint = "搜索心电图 (如: 房颤、STEMI...)"; setSingleLine(true); textSize = 14f }
        root.addView(et)
        val tv = TextView(this).apply { text = "${all.size}种"; textSize = 11f; setTextColor(Color.GRAY); setPadding(0, 6, 0, 4) }
        root.addView(tv)
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listContainer)
        root.addView(scroll)

        fun showList(filtered: List<com.mynote.android.util.ECGReference.ECG>) {
            listContainer.removeAllViews()
            val grouped = filtered.groupBy { it.category }
            for ((cat, items) in grouped) {
                val header = TextView(this).apply {
                    text = "━━ ${cat} (${items.size}) ━━"
                    textSize = 12f; setTextColor(Color.parseColor("#C62828"))
                    setPadding(4, 12, 4, 4); typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                listContainer.addView(header)
                for (item in items) {
                    val tvItem = TextView(this).apply {
                        text = "  ${item.title}"
                        textSize = 14f; setPadding(4, 8, 4, 8); setTextColor(Color.DKGRAY)
                        setOnClickListener {
                            val msg = "【${item.category}】\n\n${item.description}"
                            AlertDialog.Builder(this@SettingsActivity, R.style.RoundedDialog).setTitle(item.title)
                                .setMessage(msg)
                                .setPositiveButton("📋 复制") { _, _ ->
                                    (getSystemService(android.content.ClipboardManager::class.java))?.setPrimaryClip(android.content.ClipData.newPlainText("ecg", "${item.title}\n${item.description}"))
                                    Toast.makeText(this@SettingsActivity, "已复制", Toast.LENGTH_SHORT).show()
                                }
                                .setNeutralButton("📊 看图") { _, _ ->
                                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(item.url)))
                                }
                                .setNegativeButton("关闭", null).show()
                        }
                    }
                    listContainer.addView(tvItem)
                }
            }
        }
        showList(all)

        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim()
                val filtered = if (q.isEmpty()) all else com.mynote.android.util.ECGReference.search(q)
                tv.text = if (q.isEmpty()) "${all.size}种" else "找到${filtered.size}种"
                showList(filtered.ifEmpty { all })
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        val dlg = AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("心电图速查").setView(root)
            .setNegativeButton("关闭", null).create()
        dlg.show()
        dlg.window?.setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(), (resources.displayMetrics.heightPixels * 0.75).toInt())
    }

    // ===== 患者管理 =====
    private fun showPatientManager() {
        val pts = com.mynote.android.util.PatientManager.getAll()
        if (pts.isEmpty()) {
            AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("患者管理")
                .setMessage("暂无患者记录\n\n点击\"添加患者\"开始管理您的患者及随访提醒")
                .setPositiveButton("+ 添加患者") { _, _ -> showPatientEdit(null) }
                .setNegativeButton("关闭", null).show()
            return
        }
        val list = pts.map { "${it.name} ${it.gender} ${it.age}岁  ${it.diagnosis}  ${if (it.followUpDate.isNotBlank()) "📅${it.followUpDate}" else ""}" }
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("患者管理 (${pts.size}人)")
            .setItems(list.toTypedArray()) { _, i -> showPatientDetail(pts[i]) }
            .setPositiveButton("+ 添加") { _, _ -> showPatientEdit(null) }
            .setNegativeButton("关闭", null).show()
    }

    private fun showPatientDetail(p: com.mynote.android.util.PatientManager.Patient) {
        val info = "${p.name}  ${p.gender}  ${p.age}岁\n诊断: ${p.diagnosis}\n电话: ${p.phone}\n随访: ${if (p.followUpDate.isNotBlank()) p.followUpDate else "未设置"}\n备注: ${p.notes.ifBlank { "无" }}"
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("${p.name}")
            .setMessage(info)
            .setPositiveButton("编辑") { _, _ -> showPatientEdit(p) }
            .setNeutralButton("删除") { _, _ ->
                com.mynote.android.util.PatientManager.delete(p.id); showPatientManager()
            }
            .setNegativeButton("返回") { _, _ -> showPatientManager() }.show()
    }

    private fun showPatientEdit(existing: com.mynote.android.util.PatientManager.Patient?) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 16, 40, 0) }
        val etName = EditText(this).apply { hint = "姓名"; setSingleLine(true); if (existing != null) setText(existing.name) }
        val etAge = EditText(this).apply { hint = "年龄"; setSingleLine(true); inputType = android.text.InputType.TYPE_CLASS_NUMBER; if (existing != null) setText(existing.age) }
        val etGender = EditText(this).apply { hint = "性别 (男/女)"; setSingleLine(true); if (existing != null) setText(existing.gender) }
        val etDiag = EditText(this).apply { hint = "诊断"; setSingleLine(true); if (existing != null) setText(existing.diagnosis) }
        val etPhone = EditText(this).apply { hint = "电话"; setSingleLine(true); inputType = android.text.InputType.TYPE_CLASS_PHONE; if (existing != null) setText(existing.phone) }
        val etFollow = EditText(this).apply { hint = "随访日期 yyyy-MM-dd"; setSingleLine(true); if (existing != null) setText(existing.followUpDate) }
        val etNotes = EditText(this).apply { hint = "备注"; if (existing != null) setText(existing.notes) }
        for (et in listOf(etName, etAge, etGender, etDiag, etPhone, etFollow, etNotes)) { root.addView(et) }
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(if (existing == null) "添加患者" else "编辑患者").setView(root)
            .setPositiveButton("保存") { _, _ ->
                val p = com.mynote.android.util.PatientManager.Patient(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString().take(8),
                    name = etName.text.toString().trim(), age = etAge.text.toString().trim(), gender = etGender.text.toString().trim(),
                    diagnosis = etDiag.text.toString().trim(), phone = etPhone.text.toString().trim(),
                    followUpDate = etFollow.text.toString().trim(), notes = etNotes.text.toString().trim()
                )
                if (p.name.isBlank()) { Toast.makeText(this, "姓名必填", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                if (existing == null) com.mynote.android.util.PatientManager.add(p) else com.mynote.android.util.PatientManager.update(p)
                showPatientManager()
            }.setNegativeButton("取消") { _, _ -> showPatientManager() }.show()
    }

    // ===== 临床指南PDF库 =====
    private fun showGuidelineLibrary() {
        AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("指南速查")
            .setItems(arrayOf("📄 在线指南PDF (100+篇国内外指南)", "🩺 疾病指南参考 (960种疾病·2024-2026)")) { _, mode ->
                if (mode == 0) showGuidelinePdfList() else showGuideLookup()
            }.setNegativeButton("关闭", null).show()
    }

    private fun showGuidelinePdfList() {
        val guides = com.mynote.android.util.GuidelineLibrary.all
        val cats = com.mynote.android.util.GuidelineLibrary.categories
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 8, 16, 0) }
        val et = EditText(this).apply { hint = "搜索指南 (如: 高血压、ADA、ESC...)"; setSingleLine(true); textSize = 14f }
        root.addView(et)
        val tv = TextView(this).apply { text = "${guides.size}篇 · ${cats.size}个科室"; textSize = 11f; setTextColor(Color.GRAY); setPadding(0, 6, 0, 4) }
        root.addView(tv)

        fun showList(items: List<com.mynote.android.util.GuidelineLibrary.Guideline>) {
            AlertDialog.Builder(this, R.style.RoundedDialog).setTitle("在线指南PDF").setView(root)
                .setItems(items.map { "[${it.dept}] ${it.title} (${it.year})" }.toTypedArray()) { _, i ->
                    val g = items[i]
                    AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(g.title)
                        .setMessage("来源: ${g.source}\n科室: ${g.dept}\n年份: ${g.year}\n\n点击\"查看\"在浏览器中打开PDF")
                        .setPositiveButton("🔍 搜索最新版") { _, _ ->
                            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(g.url))) }
                            catch(e: Exception) { Toast.makeText(this@SettingsActivity, "无法打开链接", Toast.LENGTH_SHORT).show() }
                        }.setNegativeButton("关闭", null).show()
                }.setNegativeButton("关闭", null).show()
        }
        showList(guides)
        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim()
                val filtered = if (q.isEmpty()) guides else com.mynote.android.util.GuidelineLibrary.search(q)
                tv.text = if (q.isEmpty()) "${guides.size}篇" else "找到${filtered.size}篇"
                showList(filtered.ifEmpty { guides })
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun checkKnowledgeBaseUpdate() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url("https://gitee.com/api/v5/repos/mynote-kb/data/contents/version.json")
                    .get().build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = com.google.gson.Gson().fromJson(body, com.google.gson.JsonObject::class.java)
                    val remoteVersion = json.get("version")?.asString ?: prefs.kbVersion
                    val updateDesc = json.get("desc")?.asString ?: ""
                    if (remoteVersion != prefs.kbVersion) {
                        prefs.kbVersion = remoteVersion
                        withContext(Dispatchers.Main) {
                            AlertDialog.Builder(this@SettingsActivity, R.style.RoundedDialog)
                                .setTitle("知识库已更新")
                                .setMessage("新版本: v$remoteVersion\n$updateDesc\n\n重启 App 生效")
                                .setPositiveButton("确定", null).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsActivity, "已是最新版本", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "检查失败（需联网）", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun exportAllData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val exportDir = File(cacheDir, "export_$ts").also { it.mkdirs() }
                val dbPath = getDatabasePath("mynote_db")
                if (dbPath.exists()) dbPath.copyTo(File(exportDir, "mynote_db"), overwrite = true)
                listOf("mynote_db-wal", "mynote_db-shm").forEach {
                    val f = getDatabasePath(it); if (f.exists()) f.copyTo(File(exportDir, it), overwrite = true)
                }
                val notesDir = File(filesDir, "notes")
                if (notesDir.exists()) notesDir.copyRecursively(File(exportDir, "notes"), overwrite = true)
                val zipFile = File(cacheDir, "MyNote_AllData_$ts.zip")
                ZipOutputStream(zipFile.outputStream()).use { zos ->
                    exportDir.walkTopDown().filter { it.isFile }.forEach { f ->
                        zos.putNextEntry(ZipEntry(f.relativeTo(exportDir).path))
                        zos.write(f.readBytes()); zos.closeEntry()
                    }
                }
                exportDir.deleteRecursively()
                withContext(Dispatchers.Main) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(this@SettingsActivity, "$packageName.fileprovider", zipFile)
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, "导出全部数据"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@SettingsActivity, "导出失败: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private val zipPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) restoreFromZip(uri)
    }

    private fun pickZipForRestore() {
        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("从 ZIP 恢复")
            .setMessage("将覆盖当前所有数据。建议先导出备份。")
            .setPositiveButton("选择文件") { _, _ -> zipPicker.launch("application/zip") }
            .setNegativeButton("取消", null).show()
    }

    private fun restoreFromZip(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tmp = File(cacheDir, "restore_tmp").also { it.mkdirs() }
                contentResolver.openInputStream(uri)?.use { input ->
                    java.util.zip.ZipInputStream(input).use { zis ->
                        var e = zis.nextEntry
                        while (e != null) {
                            val f = File(tmp, e.name)
                            if (e.isDirectory) f.mkdirs() else { f.parentFile?.mkdirs(); f.outputStream().use { zis.copyTo(it) } }
                            zis.closeEntry(); e = zis.nextEntry
                        }
                    }
                }
                val dbDir = getDatabasePath("mynote_db").parentFile!!
                File(tmp, "mynote_db").let { if (it.exists()) it.copyTo(File(dbDir, "mynote_db"), overwrite = true) }
                listOf("mynote_db-wal", "mynote_db-shm").forEach {
                    File(tmp, it).let { src -> if (src.exists()) src.copyTo(File(dbDir, it), overwrite = true) }
                }
                File(tmp, "notes").let { if (it.exists()) { File(filesDir, "notes").deleteRecursively(); it.copyRecursively(File(filesDir, "notes"), overwrite = true) } }
                tmp.deleteRecursively()
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@SettingsActivity, R.style.RoundedDialog)
                        .setTitle("恢复完成").setMessage("数据已恢复，重启 App 生效。")
                        .setPositiveButton("退出") { _, _ -> finishAffinity() }.show()
                }
            } catch (err: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@SettingsActivity, "恢复失败: ${err.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun startDbBackup() {
        val fileName = "MyNote_db_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip"
        dbExportLauncher.launch(fileName)
    }

    private fun exportDbBackup(uri: Uri) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    BackupManager.createDatabaseBackup(this@SettingsActivity, uri)
                }
                when (result) {
                    is BackupManager.BackupResult.Success -> {
                        updateLastBackupTime()
                        Toast.makeText(this@SettingsActivity, "数据库备份成功！", Toast.LENGTH_SHORT).show()
                    }
                    is BackupManager.BackupResult.Error ->
                        Toast.makeText(this@SettingsActivity, "备份失败: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                refreshBackupInfo()
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@SettingsActivity, "备份失败: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // ===== 恢复 =====
    private fun startRestore() { restorePicker.launch("*/*") }

    private fun onRestoreFilePicked(uri: Uri) {
        val displayName = getFileName(uri).lowercase()
        if (displayName.endsWith(".zip")) {
            pendingRestoreUri = uri
            AlertDialog.Builder(this, R.style.RoundedDialog)
                .setTitle("恢复确认")
                .setMessage("将用备份文件替换当前数据库，应用会自动重启。\n\n当前数据将被覆盖，确定继续？")
                .setPositiveButton("确定恢复") { _, _ -> pendingRestoreUri?.let { doRestoreDatabase(it) } }
                .setNegativeButton("取消", null)
                .show()
        } else {
            pendingRestoreUri = uri
            AlertDialog.Builder(this, R.style.RoundedDialog)
                .setTitle("恢复模式")
                .setMessage("请选择恢复方式：")
                .setPositiveButton("合并恢复") { _, _ -> pendingRestoreUri?.let { doRestoreJson(it, false) } }
                .setNegativeButton("清空后恢复") { _, _ -> pendingRestoreUri?.let { doRestoreJson(it, true) } }
                .setNeutralButton("取消", null)
                .show()
        }
    }

    private fun doRestoreJson(uri: Uri, clearFirst: Boolean) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    BackupManager.restoreJsonBackup(this@SettingsActivity, uri, clearFirst)
                }
                when (result) {
                    is BackupManager.BackupResult.Success -> {
                        runOnUiThread {
                            updateLastBackupTime()
                            Toast.makeText(this@SettingsActivity,
                                "恢复成功！${result.noteCount}篇笔记, ${result.itemCount}个内容项, ${result.catCount}个分类",
                                Toast.LENGTH_LONG).show()
                            refreshBackupInfo()
                        }
                    }
                    is BackupManager.BackupResult.Error ->
                        runOnUiThread { Toast.makeText(this@SettingsActivity, "恢复失败: ${result.message}", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@SettingsActivity, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            pendingRestoreUri = null
        }
    }

    private fun doRestoreDatabase(uri: Uri) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    BackupManager.restoreDatabaseBackup(this@SettingsActivity, uri)
                }
                runOnUiThread {
                    when (result) {
                        is BackupManager.BackupResult.Success -> {
                            Toast.makeText(this@SettingsActivity, "数据库已恢复，正在重启...", Toast.LENGTH_SHORT).show()
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            Runtime.getRuntime().exit(0)
                        }
                        is BackupManager.BackupResult.Error ->
                            Toast.makeText(this@SettingsActivity, "恢复失败: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@SettingsActivity, "恢复失败: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            pendingRestoreUri = null
        }
    }

    // ===== 备份文件管理 =====
    private fun showManageBackupsDialog() {
        val files = BackupManager.listBackupFiles(this)
        if (files.isEmpty()) { Toast.makeText(this, "暂无备份文件", Toast.LENGTH_SHORT).show(); return }

        val items = files.map { f ->
            val date = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(f.lastModified))
            val type = if (f.isDbBackup) "[DB]" else "[JSON]"
            val size = formatFileSize(f.size)
            "$type $date  $size"
        }.toTypedArray()

        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("备份文件 (${files.size}个)")
            .setItems(items) { _, which ->
                val file = files[which]
                AlertDialog.Builder(this, R.style.RoundedDialog)
                    .setTitle(file.name)
                    .setMessage("类型: ${if (file.isDbBackup) "数据库备份" else "JSON备份"}\n大小: ${formatFileSize(file.size)}\n时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified))}")
                    .setPositiveButton("删除") { _, _ ->
                        BackupManager.deleteBackupFile(this, file.name)
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                        refreshBackupInfo()
                    }
                    .setNegativeButton("关闭", null)
                    .show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun updateLastBackupTime() {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        prefs.lastBackupTime = now
        tvLastBackup.text = now
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        return name
    }

    // ===== 图标样式切换 =====
    private val iconAliases = listOf(
        ".LauncherDefault" to "默认",
        ".LauncherCompact" to "紧凑",
        ".LauncherMinimal" to "极简",
        ".LauncherCalc" to "计算器"
    )

    private fun switchIconStyle() {
        val pm = packageManager
        val current = iconAliases.firstOrNull {
            pm.getComponentEnabledSetting(ComponentName(this, "$packageName${it.first}")) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }?.second ?: "未知"

        val items = iconAliases.mapIndexed { i, (_, label) ->
            if (label == current) "$label ✓" else label
        }.toTypedArray()

        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("切换图标 ($current)")
            .setItems(items) { _, which ->
                for ((alias, _) in iconAliases) {
                    pm.setComponentEnabledSetting(
                        ComponentName(this, "$packageName$alias"),
                        if (iconAliases[which].first == alias)
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        else
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
                Toast.makeText(this, "已切换为 ${iconAliases[which].second} 图标\n桌面图标将在几秒内刷新", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    }

    // ===== 崩溃日志 =====
    private fun refreshCrashCount() {
        val count = CrashHandler.listLogs().size
        tvCrashCount.text = "${count}条"
    }

    private fun exportCrashLogs() {
        val logs = CrashHandler.listLogs()
        if (logs.isEmpty()) {
            Toast.makeText(this, "暂无崩溃记录", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = File(cacheDir, "crash_export").also { it.mkdirs() }
                val zipFile = File(cacheDir, "CrashLogs_${
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                }.zip")
                ZipOutputStream(zipFile.outputStream()).use { zos ->
                    for (f in logs) {
                        zos.putNextEntry(ZipEntry(f.name))
                        zos.write(f.readBytes())
                        zos.closeEntry()
                    }
                }
                withContext(Dispatchers.Main) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this@SettingsActivity,
                        "$packageName.fileprovider",
                        zipFile
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "导出崩溃日志"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ===== 存储空间 =====
    private fun refreshStorageSize() {
        lifecycleScope.launch(Dispatchers.IO) {
            val size = dirSize(filesDir)
            withContext(Dispatchers.Main) {
                tvStorageSize.text = formatSize(size)
            }
        }
    }

    private fun showStorageDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val notesDir = File(filesDir, "notes")
            val notesSize = if (notesDir.exists()) dirSize(notesDir) else 0L
            val crashDir = File(filesDir, "crash_logs")
            val crashSize = if (crashDir.exists()) dirSize(crashDir) else 0L
            val total = notesSize + crashSize
            val trashCount = AppDatabase.get(this@SettingsActivity).noteDao().getTrashCount()
            val noteCount = AppDatabase.get(this@SettingsActivity).noteDao().getNoteCount()

            val msg = "笔记数量: ${noteCount} 条\n回收站: ${trashCount} 条\n\n笔记数据: ${formatSize(notesSize)}\n崩溃日志: ${formatSize(crashSize)}\n总计: ${formatSize(total)}"
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@SettingsActivity, R.style.RoundedDialog)
                    .setTitle("存储与统计")
                    .setMessage(msg)
                    .setPositiveButton("笔记统计") { _, _ -> showNoteStats() }
                    .setNegativeButton("回收站") { _, _ -> startActivity(Intent(this@SettingsActivity, com.mynote.android.ui.trash.TrashActivity::class.java)) }
                    .setNeutralButton("关闭", null)
                    .show()
            }
        }
    }

    private fun showNoteStats() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.get(this@SettingsActivity).noteDao()
            val totalNotes = dao.getNoteCount()
            val totalWords = dao.getTotalWordCount()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val weekAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 7 * 86400000L))
            val monthAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 30 * 86400000L))
            val notesThisWeek = dao.getNotesSince(weekAgo)
            val notesThisMonth = dao.getNotesSince(monthAgo)
            val activeDays = dao.getActiveDays()

            val msg = buildString {
                append("📝 总笔记: ${totalNotes} 条\n")
                append("📊 总字数: ${totalWords} 字\n")
                append("📅 本周新增: ${notesThisWeek} 条\n")
                append("📆 本月新增: ${notesThisMonth} 条\n")
                append("🔥 活跃天数: ${activeDays} 天\n")
            }
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@SettingsActivity, R.style.RoundedDialog)
                    .setTitle("笔记统计")
                    .setMessage(msg)
                    .setPositiveButton("好的", null)
                    .show()
            }
        }
    }

    /** 讯飞语音识别 API 凭证配置弹窗 */
    private fun showIatConfigDialog() {
        val p = prefs
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }
        val etAppId = EditText(this).apply { hint = "APPID"; setText(p.iatAppId); inputType = InputType.TYPE_CLASS_TEXT }
        val etApiKey = EditText(this).apply { hint = "APIKey"; setText(p.iatApiKey); inputType = InputType.TYPE_CLASS_TEXT }
        val etSecret = EditText(this).apply { hint = "APISecret"; setText(p.iatApiSecret); inputType = InputType.TYPE_CLASS_TEXT }
        container.addView(etAppId)
        container.addView(etApiKey)
        container.addView(etSecret)

        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("讯飞语音识别配置")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                p.iatAppId = etAppId.text.toString().trim()
                p.iatApiKey = etApiKey.text.toString().trim()
                p.iatApiSecret = etSecret.text.toString().trim()
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
        showKeyboard(etAppId)
    }

    private fun showKeyboard(view: View) {
        view.postDelayed({
            view.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    /** 天翼AI 识别 API 凭证配置弹窗 */
    private fun showTianyiConfigDialog() {
        val p = prefs
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }
        val etAppId = EditText(this).apply { hint = "X-APP-ID"; setText(p.tianyiAppId); inputType = InputType.TYPE_CLASS_TEXT }
        val etApiKey = EditText(this).apply { hint = "API Key"; setText(p.tianyiApiKey); inputType = InputType.TYPE_CLASS_TEXT }
        container.addView(etAppId)
        container.addView(etApiKey)

        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("天翼AI 识别配置")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                p.tianyiAppId = etAppId.text.toString().trim()
                p.tianyiApiKey = etApiKey.text.toString().trim()
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
        showKeyboard(etAppId)
    }

    /** 阿里云 Qwen 识别 API 凭证配置 */
    private fun showQwenConfigDialog() {
        val p = prefs
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
        }
        val etApiKey = EditText(this).apply { 
            hint = if (p.qwenApiKey.isEmpty()) "AccessKey ID 或 sk-xxx" else "API Key"
            setText(p.qwenApiKey)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val etSecret = EditText(this).apply {
            hint = "AccessKey Secret（sk-xxx模式留空）"
            setText(p.qwenApiSecret)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        container.addView(etApiKey)
        container.addView(etSecret)

        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("阿里云 Qwen 识别")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                p.qwenApiKey = etApiKey.text.toString().trim()
                p.qwenApiSecret = etSecret.text.toString().trim()
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
        showKeyboard(etApiKey)
    }

    /** DeepSeek AI 病历生成 API Key */
    private fun showDeepseekConfigDialog() {
        val p = prefs
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 0)
        }
        val etApiKey = EditText(this).apply {
            hint = "sk-xxx (DeepSeek API Key)"
            setText(p.deepseekApiKey)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        container.addView(etApiKey)

        AlertDialog.Builder(this, R.style.RoundedDialog)
            .setTitle("DeepSeek AI 病历生成")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                p.deepseekApiKey = etApiKey.text.toString().trim()
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
        showKeyboard(etApiKey)
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        dir.walkTopDown().forEach { f ->
            if (f.isFile) total += f.length()
        }
        return total
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / 1024 / 1024)}MB"
        else -> "${"%.2f".format(bytes.toDouble() / 1024 / 1024 / 1024)}GB"
    }
}
