package com.mynote.android.ui.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mynote.android.security.LockManager
import com.mynote.android.ui.lock.LockActivity

/**
 * Activity 基类
 * 负责:前后台切换监听 + 自动锁屏跳转
 *
 * 对应 uni-app App.vue 的 onShow/onHide 生命周期 + password-lock 路由
 */
abstract class BaseActivity : AppCompatActivity() {

    protected val lockManager: LockManager by lazy {
        LockManager(application)
    }

    /** 子类返回 true 则不参与自动锁屏(如 LockActivity 本身) */
    protected open fun exemptFromLock(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityStack.onCreated(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityStack.onDestroyed(this)
    }

    override fun onStop() {
        super.onStop()
        // 当最后一个 Activity 进入后台,记录 hide 时间
        if (ActivityStack.isLastActivity(this) && !exemptFromLock()) {
            lockManager.onAppHide()
        }
    }

    override fun onStart() {
        super.onStart()
        // 从后台回到前台,检查是否需要锁屏
        if (!exemptFromLock() && lockManager.onAppShow()) {
            startActivity(Intent(this, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
    }
}
