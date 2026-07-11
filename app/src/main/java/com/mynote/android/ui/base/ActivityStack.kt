package com.mynote.android.ui.base

import android.app.Activity
import java.util.Stack

/**
 * Activity 栈管理
 * 用于判断 App 是否进入后台(最后一个 Activity onStop)
 */
object ActivityStack {

    private val stack = Stack<Activity>()

    fun onCreated(activity: Activity) {
        stack.push(activity)
    }

    fun onDestroyed(activity: Activity) {
        stack.remove(activity)
    }

    /**
     * 当前 Activity 是否是栈中最后一个(即 App 要进入后台)
     */
    fun isLastActivity(activity: Activity): Boolean {
        return stack.size == 1 && stack.peek() === activity
    }

    fun finishAll() {
        stack.forEach { it.finish() }
        stack.clear()
    }
}
