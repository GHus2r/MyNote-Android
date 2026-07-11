package com.mynote.android.ui.main.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.mynote.android.R

/**
 * 删除密码验证弹窗 — 无按钮，输入3位自动验证
 * 密码硬编码为 '737'
 */
class DeletePasswordDialog(
    private val onVerify: () -> Unit
) : DialogFragment() {

    companion object {
        private const val PASSWORD = "737"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_delete_password, null)
        val etPassword = view.findViewById<EditText>(R.id.et_password)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
            .setView(view)
            .create()

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == PASSWORD.length) {
                    if (s.toString() == PASSWORD) {
                        onVerify()
                    }
                    dialog.dismiss()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return dialog
    }
}
