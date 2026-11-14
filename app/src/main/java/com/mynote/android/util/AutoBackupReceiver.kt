package com.mynote.android.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 定时自动云备份的广播接收器：到点触发后执行坚果云备份，并重排下一次。
 */
class AutoBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != AutoBackupScheduler.ACTION_AUTO_BACKUP) return

        val app = context.applicationContext
        val prefs = Prefs(app)

        // 开关已关 / 未配置 WebDAV → 直接返回，不重排
        if (!prefs.autoBackupSchedule || !WebDAVBackup.isConfigured(app)) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WebDAVBackup.uploadBackup(app, excludeRestored = true) { ok, _ ->
                    if (ok) {
                        prefs.lastBackupTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    }
                }
            } catch (_: Exception) {
            } finally {
                // 无论成败都重排下一次（避免一次失败导致整个定时链路断掉）
                AutoBackupScheduler.schedule(app, prefs.autoBackupHour, prefs.autoBackupMinute)
                pendingResult.finish()
            }
        }
    }
}
