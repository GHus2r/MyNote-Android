package com.mynote.android.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

/**
 * 知识库离线数据增量更新
 * 通过 WebDAV（坚果云）检查并下载更新的知识库 JSON 文件
 * 文件存放在 /MyNote/kb/ 目录下
 */
object DataUpdater {

    private const val TAG = "DataUpdater"
    private const val KB_DIR = "MyNote/kb"
    private const val VERSION_FILE = "version.txt"

    /** App 启动时调用，后台检查更新 */
    fun checkUpdate(context: Context) {
        if (!WebDAVBackup.isConfigured(context)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localVer = getLocalVersion(context)
                val remoteVer = fetchRemoteVersion(context)
                if (remoteVer.isNotEmpty() && remoteVer != localVer) {
                    Log.i(TAG, "知识库有更新: $localVer → $remoteVer")
                    downloadUpdates(context, remoteVer)
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查更新失败", e)
            }
        }
    }

    private fun getLocalVersion(context: Context): String {
        return context.getSharedPreferences("kb_version", Context.MODE_PRIVATE)
            .getString("version", "") ?: ""
    }

    private suspend fun fetchRemoteVersion(context: Context): String {
        val url = Prefs(context).webdavUrl.trimEnd('/') + "/$KB_DIR/$VERSION_FILE"
        return WebDAVBackup.downloadBackup(context, url)?.trim() ?: ""
    }

    private suspend fun downloadUpdates(context: Context, newVer: String) {
        try {
            // 下载核心数据文件列表
            val files = listOf("diseases.json", "drugs.json", "templates.json")
            for (f in files) {
                val url = Prefs(context).webdavUrl.trimEnd('/') + "/$KB_DIR/$f"
                val data = WebDAVBackup.downloadBackup(context, url) ?: continue
                // 保存到内部存储
                val outFile = java.io.File(context.filesDir, "kb_$f")
                outFile.writeText(data)
                context.getSharedPreferences("kb_version", Context.MODE_PRIVATE)
                    .edit().putString("version", newVer).apply()
                Log.i(TAG, "知识库已更新: $f")
            }
        } catch (e: Exception) {
            Log.e(TAG, "下载更新失败", e)
        }
    }
}
