package com.mynote.android.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.mynote.android.R

/**
 * 快捷方式管理。
 *
 * 方案C：伪装模式
 * - 桌面图标始终显示（不隐藏），但普通启动显示伪装计算器
 * - 桌面快捷方式（pin shortcut）启动 → 进入真实 MyNote 锁屏
 * - 不使用动态快捷方式（addDynamicShortcut），因为 MIUI 长按图标会显示它
 * - 防卸载激活/关闭时只需管理桌面快捷方式即可
 */
object IconManager {

    // ==================== 图标显隐（方案C不再需要，保留空壳兼容旧调用） ====================

    fun hide(context: Context) {
        // 方案C：不隐藏图标，改为伪装界面。此方法保留兼容性，不做任何操作。
    }

    fun show(context: Context) {
        // 方案C：不隐藏图标。此方法保留兼容性，不做任何操作。
    }

    // ==================== 桌面快捷方式 ====================

    /**
     * 创建桌面快捷方式（Android O+ requestPinShortcut）
     * 这是一个独立的桌面图标，不会出现在长按菜单中
     */
    fun createDesktopShortcut(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
            if (!shortcutManager.isRequestPinShortcutSupported) return

            val intent = Intent(context, com.mynote.android.ui.lock.LockActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("from_shortcut", true)
            }

            // 伪装为计算器名称，避免暴露 MyNote
            val shortcut = ShortcutInfo.Builder(context, "mynote_desktop_${System.currentTimeMillis()}")
                .setShortLabel("MN")
                .setLongLabel("MN")
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build()

            shortcutManager.requestPinShortcut(shortcut, null)
        } catch (_: Exception) {}
    }

    /**
     * 移除所有动态快捷方式（防卸载关闭时清理残留）
     */
    fun removeAllDynamicShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
            shortcutManager.removeAllDynamicShortcuts()
        } catch (_: Exception) {}
    }
}
