package com.mynote.android.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * 定时自动云备份的闹钟调度器。
 *
 * 采用「一次性闹钟 + 触发后重排下一次」的方式（而非 setRepeating），
 * 在 Doze 模式下也更可靠（setAndAllowWhileIdle 允许空闲时触发，每日一次无频率压力）。
 * 备份本身不要求精确到分钟，因此不申请 SCHEDULE_EXACT_ALARM 权限。
 */
object AutoBackupScheduler {

    const val ACTION_AUTO_BACKUP = "com.mynote.android.action.AUTO_BACKUP"
    private const val REQUEST_CODE = 6201

    /** 注册下一个触发时间点（今天未到则今天，否则明天） */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, next, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }

    /** 按当前开关状态重排：开启则 schedule，关闭则 cancel（供开机/启动/改设置时调用） */
    fun reschedule(context: Context) {
        val prefs = Prefs(context)
        if (prefs.autoBackupSchedule) {
            schedule(context, prefs.autoBackupHour, prefs.autoBackupMinute)
        } else {
            cancel(context)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AutoBackupReceiver::class.java).apply {
            action = ACTION_AUTO_BACKUP
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
