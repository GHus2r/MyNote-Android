package com.mynote.android.security

import android.app.Application
import android.util.Log
import com.mynote.android.util.Prefs

/**
 * 锁屏管理器
 * 对应 uni-app App.vue 的 onShow/onHide + password-lock 页的自动锁定逻辑
 *
 * 规则:
 *  - 用户离开 App(onHide)记录时间
 *  - 回到 App(onShow)时,如果超过 lockTimeMinutes 则需要重新解锁
 *  - lockTimeMinutes = -1 表示从不锁定
 *  - lockTimeMinutes = 0 表示立即锁定
 */
class LockManager(private val app: Application) {

    val prefs: Prefs = Prefs(app)

    /**
     * App 进入后台时调用
     */
    fun onAppHide() {
        prefs.appOnHideTime = System.currentTimeMillis()
        Log.d(TAG, "App 进入后台,记录时间: ${prefs.appOnHideTime}")
    }

    /**
     * App 回到前台时调用,返回是否需要锁屏
     */
    fun onAppShow(): Boolean {
        if (!prefs.passwordEnabled) return false

        // justUnlocked 标志:刚刚解锁成功,本次回前台不重复锁
        if (prefs.justUnlocked) {
            prefs.justUnlocked = false
            return false
        }

        val lockMinutes = prefs.lockTimeMinutes
        // -1 从不锁定
        if (lockMinutes == -1) return false
        // 0 立即锁定（给 2 秒阈值，避免弹框等短暂操作误锁）
        if (lockMinutes == 0) {
            val hideTime = prefs.appOnHideTime
            if (hideTime == 0L) return false
            return System.currentTimeMillis() - hideTime > 2000
        }

        val hideTime = prefs.appOnHideTime
        if (hideTime == 0L) return false

        val elapsed = System.currentTimeMillis() - hideTime
        val needLock = elapsed >= lockMinutes * 60_000L
        Log.d(TAG, "回到前台,离开 ${elapsed / 1000}s,锁定阈值 ${lockMinutes * 60}s,需锁定: $needLock")
        return needLock
    }

    /**
     * 解锁成功后调用
     */
    fun onUnlocked() {
        prefs.lastUnlockTime = System.currentTimeMillis()
        prefs.justUnlocked = true
    }

    /**
     * 验证密码
     */
    fun verifyPassword(input: String): Boolean {
        val saved = prefs.password ?: return false
        return saved == input
    }

    companion object {
        private const val TAG = "LockManager"
    }
}
