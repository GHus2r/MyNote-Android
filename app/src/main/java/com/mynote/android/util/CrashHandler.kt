package com.mynote.android.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "CrashHandler"

    private var initialized = false
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var crashDir: File? = null

    fun init(ctx: Context) {
        if (initialized) return
        initialized = true

        crashDir = File(ctx.filesDir, "crash_logs").also { it.mkdirs() }
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)

        try {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            File(crashDir, "_last_start").writeText("启动: ${fmt.format(Date())}\n" +
                "设备: ${Build.MANUFACTURER} ${Build.MODEL} | " +
                "SDK: ${Build.VERSION.SDK_INT} | " +
                "版本: ${ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName}")
        } catch (e: Exception) {
            Log.e(TAG, "写入启动标记失败", e)
        }
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        val name = try {
            val fmt = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val sw = StringWriter()
            ex.printStackTrace(PrintWriter(sw))
            val report = buildString {
                appendLine("时间: ${dateFmt.format(Date())}")
                appendLine("线程: ${thread.name}")
                appendLine("异常: ${ex.javaClass.name}: ${ex.message}")
                appendLine("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("SDK: ${Build.VERSION.SDK_INT}")
                appendLine("堆栈:")
                appendLine(sw.toString())
            }
            val n = "crash_${fmt.format(Date())}.log"
            val dir = crashDir
            if (dir != null) {
                File(dir, n).outputStream().use { out ->
                    out.write(report.toByteArray(Charsets.UTF_8))
                    // 强制刷盘，防止进程终止前未写完
                    out.fd.sync()
                }
            } else {
                Log.e(TAG, "crashDir is null, cannot write crash log")
            }
            n
        } catch (e: Throwable) {
            Log.e(TAG, "写入崩溃日志失败", e)
            null
        }

        // 交给系统默认处理器
        val dh = defaultHandler
        if (dh != null && dh !== this) {
            dh.uncaughtException(thread, ex)
        } else {
            // 没有默认处理器或不安全 → 强制结束进程
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }

    /** 列出所有崩溃日志，按时间倒序 */
    fun listLogs(): List<File> {
        val dir = crashDir ?: return emptyList()
        return dir.listFiles()
            ?.filter { it.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun getCrashDir(): File? = crashDir
}
