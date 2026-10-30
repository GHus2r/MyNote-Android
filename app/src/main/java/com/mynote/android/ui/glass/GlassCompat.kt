package com.mynote.android.ui.glass

import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.mynote.android.R
import com.mynote.android.util.Prefs

/**
 * 玻璃/模糊效果的兼容性判定。
 *
 * # 背景（鸿蒙 BUG 根因）
 * `values-v31/themes.xml` 的 GlassDialog 依赖 `android:windowBlurBehindEnabled` 做系统级
 * 背景模糊，弹窗底色则刻意做成半透明（"让模糊透出"）。
 *
 * **鸿蒙（HarmonyOS）不支持 `windowBlurBehind`**，该属性被静默忽略：模糊不生效、
 * 半透明底却照旧绘制，于是弹窗后面的页面内容直接穿透到弹窗上，与弹窗文字重叠，
 * 完全无法阅读（华为 nova 14 / 鸿蒙 7 实测）。
 *
 * # 降级策略
 * - 支持模糊的设备 → 沿用 `R.style.GlassDialog`（真模糊 + 半透明玻璃底）
 * - 不支持的设备（鸿蒙等） → 用 `R.style.GlassDialogSolid`（完全不透明底 + 标准 dim）
 *
 * # 只读检测，不修改任何状态；结果进程内缓存，避免反复反射。
 */
object GlassCompat {

    @Volatile
    private var harmonyOs: Boolean? = null

    @Volatile
    private var blurBehindSupported: Boolean? = null

    /**
     * 判断是否运行在鸿蒙（HarmonyOS）上。
     *
     * 采用三重判定，任一命中即认为是鸿蒙：
     * 1. 反射官方 `com.huawei.system.BuildEx.getOsBrand()`（最准）
     * 2. 系统属性 `hw_sc.build.platform.version`（鸿蒙特有）
     * 3. `Build.DISPLAY` / `Build.VERSION.INCREMENTAL` 含 HarmonyOS 字样
     * 4. 兜底：厂商/品牌为 HUAWEI 或 HONOR（走兼容层跑 APK 时上面几项可能失真）
     */
    fun isHarmonyOs(): Boolean {
        harmonyOs?.let { return it }
        val result = detectHarmonyOs()
        harmonyOs = result
        return result
    }

    private fun detectHarmonyOs(): Boolean {
        // 1) 官方 BuildEx（华为设备上有，EMUI 返回 "EMUI" 之类，鸿蒙返回 HarmonyOS 版本号）
        runCatching {
            val clazz = Class.forName("com.huawei.system.BuildEx")
            val method = clazz.getDeclaredMethod("getOsBrand")
            val brand = method.invoke(null) as? String
            if (!brand.isNullOrBlank() && brand.startsWith("Harmony", ignoreCase = true)) {
                return true
            }
        }

        // 2) 鸿蒙特有系统属性
        if (getSystemProperty("hw_sc.build.platform.version").isNotBlank()) return true

        // 3) 版本字符串特征
        val display = (Build.DISPLAY.orEmpty() + " " + Build.VERSION.INCREMENTAL.orEmpty())
        if (display.contains("HarmonyOS", ignoreCase = true) ||
            display.contains("Harmony", ignoreCase = true)
        ) return true

        // 4) 兜底：华为 / 荣耀（含通过兼容层运行 APK 的纯血鸿蒙）
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        return manufacturer.equals("HUAWEI", ignoreCase = true) ||
            manufacturer.equals("HONOR", ignoreCase = true) ||
            brand.equals("HUAWEI", ignoreCase = true) ||
            brand.equals("HONOR", ignoreCase = true)
    }

    /**
     * 用户在设置里手动指定的模式（`Prefs.glassMode` 非 auto 的情况）。
     *
     * @return true=强制玻璃，false=强制实心，null=跟随自动检测
     *
     * 手动设置优先级**高于**自动检测。因为自动检测无论写得多全都不可能覆盖
     * 所有 ROM —— 尤其 APK 跑在安卓兼容容器（卓易通等）中时，厂商/版本字段可能失真。
     * 漏判导致弹窗穿透时，用户能在设置里直接切 solid 自救，不必等发版。
     */
    private fun manualOverride(context: Context): Boolean? = when (Prefs(context).glassMode) {
        "glass" -> true
        "solid" -> false
        else -> null
    }

    /**
     * 系统级背景模糊（windowBlurBehind）是否真的可用。
     *
     * - 用户在设置里手动指定过 → 直接用该值
     * - API < 31：无该特性 → false
     * - 鸿蒙：明确不支持 → false
     * - API ≥ 33：以官方 `WindowManager.isCrossWindowBlurEnabled` 为准
     *   （用户关闭"模糊"效果、省电模式、设备不支持时都会返回 false）
     * - API 31/32：无公开查询接口，非鸿蒙则保守认为支持
     */
    fun isBlurBehindSupported(context: Context): Boolean {
        manualOverride(context)?.let { return it }
        blurBehindSupported?.let { return it }
        val result = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> false
            isHarmonyOs() -> false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.isCrossWindowBlurEnabled == true
            }
            else -> true
        }
        blurBehindSupported = result
        return result
    }

    /** Haze（Compose 模糊，基于 RenderEffect）是否可用。判定口径同系统模糊。 */
    fun isHazeSupported(context: Context): Boolean = isBlurBehindSupported(context)

    /** 当前设备应使用的玻璃弹窗主题：支持模糊走玻璃版，否则走实心版。 */
    fun dialogTheme(context: Context): Int =
        if (isBlurBehindSupported(context)) R.style.GlassDialog else R.style.GlassDialogSolid

    private fun getSystemProperty(key: String): String {
        // 1) 反射 android.os.SystemProperties.get（隐藏 API，不必 fork 进程）
        val viaReflect = runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(null, key) as? String
        }.getOrNull()
        if (!viaReflect.isNullOrBlank()) return viaReflect

        // 2) 回退：getprop 命令
        return runCatching {
            val process = Runtime.getRuntime().exec("getprop $key")
            val value = process.inputStream.bufferedReader().use { it.readText().trim() }
            process.destroy()
            value
        }.getOrDefault("")
    }
}
