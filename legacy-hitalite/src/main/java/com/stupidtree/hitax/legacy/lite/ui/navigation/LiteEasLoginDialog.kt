package com.stupidtree.hitax.legacy.lite.ui.navigation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.legacy.lite.databinding.LiteNavEasLoginBinding

/**
 * HITA-L 风格的教务登录底部弹层。
 *
 * 行为对齐 HITA-L：深圳 API 登录优先，其他校区提示使用网页登录；
 * 勾选协议后才能提交；失败展示错误；网页登录转交宿主 Web 登录 Activity。
 *
 * 前置条件：LiteHostBridge.easAuth 非空；为空时只保留网页登录。
 * 后置条件：成功回调后关闭；失败保留输入并显示错误。
 * 不变式：不保存账号密码，不创建第二套登录仓库。
 */
class LiteEasLoginDialog : BottomSheetDialogFragment() {

    private var binding: LiteNavEasLoginBinding? = null
    private var request: LiveData<DataState<Boolean>>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val b = LiteNavEasLoginBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        b.liteLoginCampus.setOnCheckedChangeListener { _, checkedId ->
            val shenzhen = checkedId == R.id.lite_login_campus_shenzhen
            b.liteLoginCampusHint.visibility = if (shenzhen) View.GONE else View.VISIBLE
            b.liteLoginApi.isEnabled = canSubmit(shenzhen)
        }
        b.liteLoginAgree.setOnCheckedChangeListener { _, _ ->
            b.liteLoginApi.isEnabled = canSubmit(isShenzhen())
        }
        b.liteLoginUsername.addTextChangedListener(SimpleWatcher { refreshSubmit() })
        b.liteLoginPassword.addTextChangedListener(SimpleWatcher { refreshSubmit() })
        b.liteLoginApi.setOnClickListener { submit() }
        b.liteLoginWeb.setOnClickListener {
            LiteHostBridge.openLogin?.invoke(requireActivity())
            dismissAllowingStateLoss()
        }
        b.liteLoginLater.setOnClickListener { dismissAllowingStateLoss() }
        refreshSubmit()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
        BottomSheetBehavior.from(
            dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)
        ).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun isShenzhen(): Boolean =
        binding?.liteLoginCampus?.checkedRadioButtonId == R.id.lite_login_campus_shenzhen

    private fun canSubmit(shenzhen: Boolean): Boolean {
        val b = binding ?: return false
        val username = b.liteLoginUsername.text?.toString()?.trim().orEmpty()
        val password = b.liteLoginPassword.text?.toString().orEmpty()
        return shenzhen && b.liteLoginAgree.isChecked && username.isNotEmpty() && password.isNotEmpty()
    }

    private fun refreshSubmit() {
        binding?.liteLoginApi?.isEnabled = canSubmit(isShenzhen())
    }

    private fun submit() {
        val b = binding ?: return
        val auth = LiteHostBridge.easAuth ?: run {
            showError(getString(R.string.lite_login_error_default))
            return
        }
        val username = b.liteLoginUsername.text?.toString()?.trim().orEmpty()
        val password = b.liteLoginPassword.text?.toString().orEmpty()
        if (username.isEmpty() || password.isEmpty()) return
        setLoading(true)
        b.liteLoginError.visibility = View.GONE
        request = auth.login(username, password).also {
            it.observe(viewLifecycleOwner) { state -> handleResult(state) }
        }
    }

    private fun handleResult(state: DataState<Boolean>?) {
        val b = binding ?: return
        if (state == null) return
        if (state.state == DataState.STATE.SUCCESS && state.data == true) {
            setLoading(false)
            dismissAllowingStateLoss()
            return
        }
        setLoading(false)
        val message = state.message.orEmpty()
        showError(
            when {
                message.contains("timeout", true) || message.contains("timed out", true) ->
                    getString(R.string.lite_login_error_network)
                message.isBlank() -> getString(R.string.lite_login_error_default)
                else -> message
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        val b = binding ?: return
        b.liteLoginApi.isEnabled = !loading && canSubmit(isShenzhen())
        b.liteLoginApi.text = if (loading) {
            getString(R.string.lite_login_loading)
        } else {
            getString(R.string.lite_login_api)
        }
        b.liteLoginUsername.isEnabled = !loading
        b.liteLoginPassword.isEnabled = !loading
        b.liteLoginAgree.isEnabled = !loading
        b.liteLoginWeb.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding?.liteLoginError?.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private class SimpleWatcher(private val action: () -> Unit) :
        android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: android.text.Editable?) = action()
    }
}
