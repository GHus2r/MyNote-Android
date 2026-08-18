package com.mynote.android.ui.main.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynote.android.ui.glass.GlassDialogFragment
import com.mynote.android.ui.glass.GlassPanel

/**
 * 删除密码验证弹窗 — Haze 玻璃材质版
 * 无按钮，输入 3 位自动验证；密码硬编码为 '737'
 */
class DeletePasswordDialog(
    private val onVerify: () -> Unit
) : GlassDialogFragment() {

    private companion object {
        private const val PASSWORD = "737"
    }

    @Composable
    override fun Content() {
        var pwd by remember { mutableStateOf("") }

        // 与旧版一致：满 3 位即校验并关闭
        LaunchedEffect(pwd) {
            if (pwd.length == PASSWORD.length) {
                if (pwd == PASSWORD) onVerify()
                dismiss()
            }
        }

        GlassPanel {
            Text(
                text = "输入删除密码",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF212121)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = pwd,
                onValueChange = { if (it.length <= PASSWORD.length) pwd = it },
                singleLine = true,
                placeholder = { Text("3 位密码") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
