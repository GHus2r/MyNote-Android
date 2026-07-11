package com.mynote.android.ui.lock

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.mynote.android.R
import com.mynote.android.ui.lock.widget.FingerprintRingView
import com.mynote.android.ui.lock.widget.FloatingBubbleKeypad
import com.mynote.android.ui.lock.widget.FlowingGradientView
import com.mynote.android.ui.main.MainActivity
import com.mynote.android.util.Prefs

class LockActivity : AppCompatActivity() {

    private val prefs: Prefs by lazy { Prefs(this) }

    private lateinit var videoBg: FlowingGradientView
    private lateinit var bubbleKeypad: FloatingBubbleKeypad
    private lateinit var btnFingerprint: FingerprintRingView

    private val passwordBuilder = StringBuilder()
    private var maxPasswordLength = 4
    private var tapCount = 0
    private var lastTapTime = 0L
    private var fingerprintConsumed = false

    // 伪装计算器模式标记
    private var fakeCalcShown = false

    // 键盘聚散控制
    private var emptyTapCount = 0
    private var lastEmptyTapTime = 0L
    private var scatterRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ===== 活跃会话检测 =====
        // 10分钟内刚解锁过 + 不是从桌面图标主动打开 → 直接消失（防止查看器返回路径出现）
        val sessionAge = System.currentTimeMillis() - prefs.appSessionTimestamp
        val isIconLaunch = intent?.action == Intent.ACTION_MAIN &&
            intent?.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
        if (sessionAge in 1..600_000 && !isIconLaunch) {
            finish()
            return
        }

        // ===== 方案C：伪装模式判断 =====
        val fromShortcut = intent?.getBooleanExtra("from_shortcut", false) == true
        val wasShortcutSession = prefs.wasShortcutSession

        if (prefs.antiUninstallEnabled && !fromShortcut && !wasShortcutSession) {
            fakeCalcShown = true
            showFakeCalculator()
            return
        }

        // 标记：本次通过快捷方式进入
        if (fromShortcut) {
            prefs.wasShortcutSession = true
        } else {
            prefs.wasShortcutSession = false
        }

        setContentView(R.layout.activity_lock)
        // 覆盖 manifest 中的伪装 label，让最近任务列表显示真实名称
        title = getString(R.string.app_name)

        // Deep Link 处理：从浏览器等外部入口打开时，intent.data 不为 null
        // 判断是否是 Deep Link 启动，如果是且未设置密码，直接进主页
        val isFromDeepLink = intent?.data != null

        if (!prefs.passwordEnabled) {
            enterMain()
            return
        }

        videoBg = findViewById(R.id.video_bg)
        bubbleKeypad = findViewById(R.id.bubble_keypad)
        btnFingerprint = findViewById(R.id.btn_fingerprint)

        // N字碎片背景自动运行

        val savedPwd = prefs.password ?: ""
        maxPasswordLength = savedPwd.length.coerceIn(4, 8)

        initBubbleKeypad()
        initFingerprint()
    }

    // ===== 4-Tap Gesture (仅首次，指纹/密码使用后禁用) =====
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (fingerprintConsumed) return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            if (now - lastTapTime > 1500) tapCount = 0
            lastTapTime = now
            tapCount++
            if (tapCount >= 4) { tapCount = 0; triggerFingerprint() }
        }
        return super.onTouchEvent(event)
    }

    private fun triggerFingerprint() {
        val canBio = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        if (prefs.fingerprintEnabled && canBio) {
            btnFingerprint.visibility = View.VISIBLE
            btnFingerprint.postDelayed({ startSystemFingerprint() }, 300)
        } else {
            showBubbleKeypad()
        }
    }

    // ===== Fingerprint =====
    private fun initFingerprint() {
        btnFingerprint.setOnClickListener { startSystemFingerprint() }
    }

    private fun startSystemFingerprint() {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlockSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    btnFingerprint.visibility = View.GONE
                    showBubbleKeypad()
                }
                override fun onAuthenticationFailed() {
                    fingerprintConsumed = true
                }
            })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("指纹验证").setSubtitle("使用已录入的指纹解锁")
            .setNegativeButtonText("使用密码").build())
    }

    // ===== Bubble Keypad =====
    private fun initBubbleKeypad() {
        bubbleKeypad.onDigitTap = { digit ->
            if (passwordBuilder.length < maxPasswordLength) {
                passwordBuilder.append(digit)
                if (passwordBuilder.length == maxPasswordLength) checkPassword()
            }
        }
        bubbleKeypad.onEmptyTap = {
            handleEmptyTap()
        }
    }

    private fun handleEmptyTap() {
        cancelScatterTimer()

        val now = System.currentTimeMillis()
        if (now - lastEmptyTapTime > 2000) emptyTapCount = 0
        lastEmptyTapTime = now
        emptyTapCount++

        if (bubbleKeypad.isArranged) {
            // 键盘已排列状态下再次点击 → 散开
            bubbleKeypad.scatterAway()
            emptyTapCount = 0
            return
        }

        bubbleKeypad.turboBoost()

        // 连续 5 次空白点击 → 聚合成键盘
        if (emptyTapCount >= 5) {
            bubbleKeypad.arrangeToKeypad()
            videoBg.gatherToN()
            emptyTapCount = 0
            // 聚合后 4 秒无操作自动散开
            startScatterTimer()
        }
    }

    private fun startScatterTimer() {
        scatterRunnable = Runnable {
            bubbleKeypad.scatterAway()
            videoBg.scatterOut()
        }
        videoBg.postDelayed(scatterRunnable, 4000)
    }

    private fun cancelScatterTimer() {
        scatterRunnable?.let { videoBg.removeCallbacks(it) }
        scatterRunnable = null
    }

    private fun showBubbleKeypad() {
        fingerprintConsumed = true
        btnFingerprint.visibility = View.GONE
        bubbleKeypad.visibility = View.VISIBLE
        bubbleKeypad.show()
    }

    private fun checkPassword() {
        if (prefs.password == passwordBuilder.toString()) {
            onUnlockSuccess()
        } else {
            bubbleKeypad.postDelayed({ passwordBuilder.clear() }, 400)
            bubbleKeypad.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    private fun onUnlockSuccess() {
        prefs.justUnlocked = true
        prefs.lastUnlockTime = System.currentTimeMillis()
        bubbleKeypad.hide()
        enterMain()
    }

    private fun enterMain() {
        prefs.wasShortcutSession = false
        prefs.appSessionTimestamp = System.currentTimeMillis()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val fromShortcut = intent.getBooleanExtra("from_shortcut", false)
        if (fakeCalcShown) {
            // 伪装模式中收到快捷方式 → 切换到真实锁屏
            if (fromShortcut) {
                fakeCalcShown = false
                recreate()
            }
            return
        }
        // 正常锁屏中收到非快捷方式 → 切换为伪装计算器
        if (prefs.antiUninstallEnabled && !fromShortcut) {
            fakeCalcShown = true
            showFakeCalculator()
        }
    }

    override fun onBackPressed() {
        // 伪装计算器：直接关闭，不进入锁屏流程
        if (fakeCalcShown) {
            finish()
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubbleKeypad.isInitialized) {
            bubbleKeypad.hide()
        }
    }

    // ===== 方案C：伪装计算器 =====

    private lateinit var fakeDisplay: android.widget.TextView
    private var fakeLeftNum = ""
    private var fakeRightNum = ""
    private var fakeOp = ""
    private var fakeNewNumber = true

    private fun showFakeCalculator() {
        setContentView(R.layout.activity_fake_calculator)
        title = "计算器"

        fakeDisplay = findViewById(R.id.fake_display)

        // 数字按钮
        val digitIds = intArrayOf(
            R.id.fake_btn_0, R.id.fake_btn_1, R.id.fake_btn_2, R.id.fake_btn_3, R.id.fake_btn_4,
            R.id.fake_btn_5, R.id.fake_btn_6, R.id.fake_btn_7, R.id.fake_btn_8, R.id.fake_btn_9
        )
        digitIds.forEachIndexed { digit, id ->
            findViewById<android.widget.Button>(id).setOnClickListener { onFakeDigit(digit) }
        }

        // 运算符
        findViewById<android.widget.Button>(R.id.fake_btn_add).setOnClickListener { onFakeOp("+") }
        findViewById<android.widget.Button>(R.id.fake_btn_sub).setOnClickListener { onFakeOp("-") }
        findViewById<android.widget.Button>(R.id.fake_btn_mul).setOnClickListener { onFakeOp("×") }
        findViewById<android.widget.Button>(R.id.fake_btn_div).setOnClickListener { onFakeOp("÷") }

        // 等号
        findViewById<android.widget.Button>(R.id.fake_btn_eq).setOnClickListener { onFakeEquals() }

        // 清除
        findViewById<android.widget.Button>(R.id.fake_btn_clear).setOnClickListener {
            fakeLeftNum = ""
            fakeRightNum = ""
            fakeOp = ""
            fakeNewNumber = true
            fakeDisplay.text = "0"
        }

        // 退格
        findViewById<android.widget.Button>(R.id.fake_btn_del).setOnClickListener {
            if (fakeNewNumber) {
                fakeLeftNum = if (fakeLeftNum.length > 1) fakeLeftNum.dropLast(1) else ""
                fakeDisplay.text = fakeLeftNum.ifEmpty { "0" }
            } else {
                fakeRightNum = if (fakeRightNum.length > 1) fakeRightNum.dropLast(1) else ""
                fakeDisplay.text = fakeRightNum.ifEmpty { "0" }
            }
        }
    }

    private fun onFakeDigit(digit: Int) {
        if (fakeNewNumber) {
            fakeLeftNum += digit.toString()
            fakeDisplay.text = fakeLeftNum
        } else {
            fakeRightNum += digit.toString()
            fakeDisplay.text = fakeRightNum
        }
    }

    private fun onFakeOp(op: String) {
        if (fakeLeftNum.isNotEmpty()) {
            fakeOp = op
            fakeNewNumber = false
        }
    }

    private fun onFakeEquals() {
        if (fakeLeftNum.isEmpty() || fakeRightNum.isEmpty() || fakeOp.isEmpty()) return
        val a = fakeLeftNum.toDoubleOrNull() ?: return
        val b = fakeRightNum.toDoubleOrNull() ?: return
        val result = when (fakeOp) {
            "+" -> a + b
            "-" -> a - b
            "×" -> a * b
            "÷" -> if (b != 0.0) a / b else return
            else -> return
        }
        val display = if (result == result.toLong().toDouble()) result.toLong().toString() else result.toString()
        fakeDisplay.text = display
        fakeLeftNum = display
        fakeRightNum = ""
        fakeOp = ""
        fakeNewNumber = true
    }
}
