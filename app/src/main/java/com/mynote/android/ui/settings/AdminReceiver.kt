package com.mynote.android.ui.settings

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.mynote.android.util.IconManager
import com.mynote.android.util.Prefs

/**
 * 防卸载设备管理员接收器
 *
 * 激活后系统拒绝直接卸载，必须先停用管理员。
 * 方案C：不隐藏图标，改为伪装计算器界面。
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Prefs(context).antiUninstallEnabled = true
        Toast.makeText(context, "防卸载保护已激活，桌面图标将显示伪装界面", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        val prefs = Prefs(context)
        prefs.antiUninstallEnabled = false

        // 移除所有动态快捷方式（清理残留）
        IconManager.removeAllDynamicShortcuts(context)

        // 管理员被撤销 → 自动开启密码锁屏，锁定应用
        prefs.passwordEnabled = true
        prefs.justUnlocked = false

        Toast.makeText(context, "防卸载保护已关闭，应用已自动锁定", Toast.LENGTH_LONG).show()
    }

    /**
     * 用户尝试在系统设置中停用管理员时的阻止提示
     */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "停用后应用将自动锁定，伪装界面解除。\n确定继续？"
    }

    override fun onPasswordChanged(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordChanged(context, intent, userHandle)
    }

    override fun onPasswordFailed(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, userHandle)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)
    }
}
