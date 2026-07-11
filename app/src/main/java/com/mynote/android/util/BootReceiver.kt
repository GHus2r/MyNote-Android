package com.mynote.android.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mynote.android.ui.recording.QuickRecordService

/**
 * 开机自启动摇摆录音服务
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = Prefs(context)
            if (prefs.quickRecordEnabled) {
                QuickRecordService.start(context)
            }
        }
    }
}
