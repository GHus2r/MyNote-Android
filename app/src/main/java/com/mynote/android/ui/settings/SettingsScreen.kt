package com.mynote.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** 设置页 UI 状态 */
data class SettingsState(
    val passwordEnabled: Boolean = false,
    val fingerprintEnabled: Boolean = false,
    val antiUninstallEnabled: Boolean = false,
    val quickRecordEnabled: Boolean = false,
    val canBio: Boolean = false,
    val lockTimeLabel: String = "立即",
    val shakeSensitiveLabel: String = "灵敏",
    val lastBackupLabel: String = "从未备份",
    val backupCountLabel: String = "0个",
    val patientCountLabel: String = "0人",
    val storageSizeLabel: String = "计算中...",
    val crashCountLabel: String = "0条",
    val antiUninstallHint: String = "激活后可防止恶意卸载"
)

/**
 * 设置页「设置项列表 + 玻璃顶栏」（backdrop 真玻璃，透出滚动内容）。
 * 顶栏按钮（返回/标题）是 View 层，透明叠在本组件上方（见 activity_settings.xml）。
 */
@Composable
fun SettingsScreen(
    state: SettingsState,
    onPasswordToggle: (Boolean) -> Unit,
    onChangePassword: () -> Unit,
    onFingerprintToggle: (Boolean) -> Unit,
    onLockTimeClick: () -> Unit,
    onAntiUninstallToggle: (Boolean) -> Unit,
    onLocalBackupClick: () -> Unit,
    onDbBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onManageBackupsClick: () -> Unit,
    onCreateShortcutClick: () -> Unit,
    onExportAllClick: () -> Unit,
    onRestoreZipClick: () -> Unit,
    onPatientMgrClick: () -> Unit,
    onQuickRecordToggle: (Boolean) -> Unit,
    onShakeSensitiveClick: () -> Unit,
    onStorageClick: () -> Unit,
    onIatConfigClick: () -> Unit,
    onTianyiConfigClick: () -> Unit,
    onQwenConfigClick: () -> Unit,
    onDeepseekConfigClick: () -> Unit,
    onCrashLogsClick: () -> Unit
) {
    // 顶栏延伸到状态栏下方（与 View 层 LinearLayout 的 fitsSystemWindows 对齐）
    val view = LocalView.current
    val statusBarTop = with(LocalDensity.current) {
        ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top
            ?.toDp() ?: 0.dp
    }
    val topBarHeight = 48.dp + statusBarTop

    val isDark = isSystemInDarkTheme()
    // 页面底色：浅浅的紫→白渐变
    val bgBrush = Brush.linearGradient(
        if (isDark) {
            listOf(
                Color(0xFF4A148C), // 深紫
                Color(0xFF311B92), // 紫 900
                Color(0xFF121212)  // 深灰黑
            )
        } else {
            listOf(
                Color(0xFFD1C4E9), // 淡紫
                Color(0xFFEDE7F6), // 极淡紫
                Color(0xFFFFFFFF)  // 白
            )
        }
    )

    Box(Modifier.fillMaxSize()) {
        // 1) 页面背景（页面"底色"）：紫白渐变；既可见又拍进 backdrop 层
        //    ⚠️ rememberLayerBackdrop 必须在 Box 内，与 layerBackdrop 同坐标空间
        val backdrop = rememberLayerBackdrop {
            drawRect(bgBrush)
            drawContent()
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(bgBrush)
                .layerBackdrop(backdrop)
        )

        // 2) 设置项列表（玻璃卡片透出上面的页面背景）
        //    顶部不留 contentPadding，让第一项在初始位置被顶栏遮住；
        //    用 spacer item 把真实内容推到顶栏下方，滚动时内容会从顶栏下方穿过。
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 占位：让真实内容初始时位于透明顶栏下方
            item(key = "top_spacer") { Spacer(Modifier.height(topBarHeight)) }

            // 安全设置
            item(key = "h1") { GroupHeader("安全设置") }
            item(key = "password") {
                SwitchItem("密码保护", checked = state.passwordEnabled, onChecked = onPasswordToggle, backdrop = backdrop)
            }
            if (state.passwordEnabled) {
                item(key = "change_pwd") {
                    ClickItem("修改密码", onClick = onChangePassword, backdrop = backdrop)
                }
                item(key = "fingerprint") {
                    SwitchItem(
                        "指纹解锁",
                        checked = state.fingerprintEnabled,
                        onChecked = onFingerprintToggle,
                        enabled = state.canBio,
                        hint = if (!state.canBio) "设备不支持硬件" else null,
                        backdrop = backdrop
                    )
                }
            }
            item(key = "lock_time") {
                ValueItem("自动锁定", value = state.lockTimeLabel, onClick = onLockTimeClick, backdrop = backdrop)
            }

            // 安全保护
            item(key = "h2") { GroupHeader("安全保护") }
            item(key = "anti_uninstall") {
                SwitchItem("防卸载保护", checked = state.antiUninstallEnabled, onChecked = onAntiUninstallToggle, hint = state.antiUninstallHint, backdrop = backdrop)
            }

            // 备份
            item(key = "h3") { GroupHeader("备份") }
            item(key = "last_backup") { DisplayItem("上次备份", value = state.lastBackupLabel, backdrop = backdrop) }
            item(key = "local_backup") { ClickItem("完整备份 (JSON)", sub = "含分类+笔记+内容", onClick = onLocalBackupClick, backdrop = backdrop) }
            item(key = "db_backup") { ClickItem("快速备份 (DB)", sub = "直接打包数据库", onClick = onDbBackupClick, backdrop = backdrop) }
            item(key = "restore_backup") { ClickItem("从备份恢复", sub = "选择备份文件", onClick = onRestoreBackupClick, backdrop = backdrop) }
            item(key = "manage_backups") { ValueItem("管理备份文件", value = state.backupCountLabel, onClick = onManageBackupsClick, backdrop = backdrop) }
            item(key = "create_shortcut") { ClickItem("创建桌面快捷方式", sub = "防卸载备用入口", onClick = onCreateShortcutClick, backdrop = backdrop) }
            item(key = "export_all") { ClickItem("导出全部数据 (ZIP)", sub = "数据库+附件+笔记", onClick = onExportAllClick, backdrop = backdrop) }
            item(key = "restore_zip") { ClickItem("从 ZIP 恢复", sub = "选择 .zip 文件恢复", onClick = onRestoreZipClick, backdrop = backdrop) }

            // 患者管理
            item(key = "h4") { GroupHeader("患者管理") }
            item(key = "patient_mgr") { ValueItem("患者信息管理", value = state.patientCountLabel, onClick = onPatientMgrClick, backdrop = backdrop) }

            // AI 与语音
            item(key = "h5") { GroupHeader("AI 与语音") }
            item(key = "quick_record") { SwitchItem("摇一摇快速录音", checked = state.quickRecordEnabled, onChecked = onQuickRecordToggle, hint = "锁屏也可用", backdrop = backdrop) }
            item(key = "shake_sensitive") { ValueItem("灵敏度", value = state.shakeSensitiveLabel, onClick = onShakeSensitiveClick, backdrop = backdrop) }

            // 存储与清理
            item(key = "h6") { GroupHeader("存储与清理") }
            item(key = "storage") { ValueItem("占用空间", value = state.storageSizeLabel, onClick = onStorageClick, backdrop = backdrop) }

            // 开发工具
            item(key = "h7") { GroupHeader("开发工具") }
            item(key = "iat_config") { ClickItem("讯飞语音识别配置", onClick = onIatConfigClick, backdrop = backdrop) }
            item(key = "tianyi_config") { ClickItem("天翼AI 识别配置", onClick = onTianyiConfigClick, backdrop = backdrop) }
            item(key = "qwen_config") { ClickItem("阿里云 Qwen 识别", onClick = onQwenConfigClick, backdrop = backdrop) }
            item(key = "deepseek_config") { ClickItem("DeepSeek AI 病历生成", onClick = onDeepseekConfigClick, backdrop = backdrop) }
            item(key = "crash_logs") { ValueItem("导出崩溃日志", value = state.crashCountLabel, onClick = onCrashLogsClick, backdrop = backdrop) }
        }

        // 3) 圆角玻璃顶栏：底部 20dp 圆角 + 真玻璃，滚动内容从下方穿过时被模糊透出；
        //    1dp 描边勾出圆角轮廓，View 层文字/箭头悬浮其上。
        val topBarShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topBarHeight)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { topBarShape },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(6f.dp.toPx(), 14f.dp.toPx())
                    }
                )
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF7C4DFF).copy(alpha = 0.30f),
                    topBarShape
                )
        )
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = SettingsTitleColor().copy(alpha = 0.72f),
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
    )
}

/**
 * 玻璃设置卡片：透明真玻璃，透出页面背景（backdrop 层的渐变）。
 * 与 GlassDemoActivity 的 GlassCard 同款模式：drawBackdrop(vibrancy+blur+lens) + 淡描边。
 * 文字色自适应：日间黑色，深紫背景（夜间）用白字。
 */
@Composable
private fun SettingsTitleColor(): Color =
    if (isSystemInDarkTheme()) Color.White else Color(0xFF212121)

@Composable
private fun GlassCard(
    backdrop: Backdrop,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val dark = isSystemInDarkTheme()
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(6f.dp.toPx(), 14f.dp.toPx())
                }
            )
            .border(
                1.dp,
                if (dark) Color.White.copy(alpha = 0.35f) else Color(0xFF7C4DFF).copy(alpha = 0.30f),
                shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        content()
    }
}

@Composable
private fun SwitchItem(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    backdrop: Backdrop,
    enabled: Boolean = true,
    hint: String? = null
) {
    val titleColor = SettingsTitleColor()
    val dark = isSystemInDarkTheme()
    GlassCard(backdrop = backdrop) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = titleColor, modifier = Modifier.weight(1f))
                Switch(
                    checked = checked,
                    onCheckedChange = onChecked,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = if (dark) Color.White else Color(0xFF7C4DFF),
                        checkedThumbColor = if (dark) Color(0xFF7C4DFF) else Color.White,
                        uncheckedTrackColor = if (dark) Color.White.copy(alpha = 0.35f) else Color(0xFFB39DDB),
                        uncheckedThumbColor = if (dark) Color.White else Color.White
                    )
                )
            }
            if (hint != null) {
                Text(hint, fontSize = 11.sp, color = titleColor.copy(alpha = 0.72f), modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun ClickItem(
    title: String,
    sub: String? = null,
    onClick: () -> Unit,
    backdrop: Backdrop
) {
    val titleColor = SettingsTitleColor()
    GlassCard(backdrop = backdrop, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = titleColor, modifier = Modifier.weight(1f))
            if (sub != null) {
                Text(sub, fontSize = 12.sp, color = titleColor.copy(alpha = 0.72f))
                Spacer(Modifier.width(8.dp))
            }
            Text(">", fontSize = 18.sp, color = titleColor.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun ValueItem(
    title: String,
    value: String,
    onClick: () -> Unit,
    backdrop: Backdrop
) {
    val titleColor = SettingsTitleColor()
    GlassCard(backdrop = backdrop, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = titleColor, modifier = Modifier.weight(1f))
            Text(value, fontSize = 14.sp, color = titleColor.copy(alpha = 0.80f))
            Spacer(Modifier.width(8.dp))
            Text(">", fontSize = 18.sp, color = titleColor.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun DisplayItem(
    title: String,
    value: String,
    backdrop: Backdrop
) {
    val titleColor = SettingsTitleColor()
    GlassCard(backdrop = backdrop) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = titleColor, modifier = Modifier.weight(1f))
            Text(value, fontSize = 14.sp, color = titleColor.copy(alpha = 0.80f))
        }
    }
}
