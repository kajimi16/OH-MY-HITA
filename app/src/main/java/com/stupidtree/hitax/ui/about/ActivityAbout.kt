package com.stupidtree.hitax.ui.about

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.Html
import android.view.HapticFeedbackConstants
import android.widget.Toast
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.R
import com.stupidtree.hitax.databinding.ActivityAboutBinding
import com.stupidtree.hitax.utils.ActivityUtils
import com.stupidtree.hitax.utils.ImageUtils
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.widgets.PopUpText

@Suppress("DEPRECATION")
class ActivityAbout: BaseActivity<AboutViewModel,ActivityAboutBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWindowParams(true,false,false)
        setToolbarActionBack(binding.toolbar)
    }

    override fun initViews() {
        viewModel.aboutPageLiveData.observe(this){ state ->
            state.data?.let {
                binding.aboutInfo.text = Html.fromHtml(it)
            }
        }
        binding.privacyProtocol.setOnClickListener {
             UserAgreementDialog().show(
                 supportFragmentManager,"a"
             )
        }
        binding.exportDiagnostics.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            pickExportDirectory()
        }
        binding.check.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            binding.check.startAnimation()
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    0
                ).longVersionCode
            } else {
                packageManager.getPackageInfo(
                    packageName,
                    0
                ).versionCode.toLong()
            }
            viewModel.checkForUpdate(code)
        }
        viewModel.checkUpdateResult.observe(this){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                binding.check.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                binding.check.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            val bitmap =
                ImageUtils.getResourceBitmap(getThis(), if (it.state==DataState.STATE.SUCCESS)
                    R.drawable.ic_baseline_done_24 else R.drawable.ic_baseline_error_24)
            binding.check.doneLoadingAnimation(
                getColorPrimary(), bitmap
            )
            binding.check.postDelayed({
                binding.check.revertAnimation()
            }, 600)
            if (it.state == DataState.STATE.SUCCESS) {
                it.data?.let { cr ->
                    if (cr.shouldUpdate) {
                        ActivityUtils.showUpdateNotificationForce(cr,this)
                    }else{
                        Toast.makeText(this,R.string.already_up_to_date,Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_EXPORT_DIAGNOSTICS) return
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) {
            Toast.makeText(this, "已取消导出", Toast.LENGTH_SHORT).show()
            return
        }
        writeDiagnosticsToTree(uri)
    }

    /** 让用户选一个目录；没有文档选择器时退回应用外部目录。 */
    private fun pickExportDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, REQUEST_EXPORT_DIAGNOSTICS)
        } catch (t: Throwable) {
            writeDiagnosticsToDirectory(getExternalFilesDir(null) ?: filesDir)
        }
    }

    private fun exportFiles(): Map<String, String>? {
        val app = application as? HApplication
        val files = app?.let { runCatching { it.buildDiagnosticsExport() }.getOrNull() }
        if (files == null) {
            Toast.makeText(this, "导出失败：无法生成诊断内容", Toast.LENGTH_LONG).show()
        }
        return files
    }

    private fun writeDiagnosticsToTree(treeUri: Uri) {
        val files = exportFiles() ?: return
        try {
            val parent = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri),
            )
            files.forEach { (name, json) ->
                val document = DocumentsContract.createDocument(
                    contentResolver, parent, "application/json", name,
                ) ?: error("无法创建 $name")
                contentResolver.openOutputStream(document)?.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("无法写入 $name")
            }
            Toast.makeText(this, "已导出 ${files.keys.joinToString("、")}", Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            Toast.makeText(
                this,
                "导出失败：${t.message ?: t.javaClass.simpleName}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun writeDiagnosticsToDirectory(base: java.io.File) {
        val files = exportFiles() ?: return
        runCatching {
            val dir = java.io.File(base, "diagnostics").apply { mkdirs() }
            files.forEach { (name, json) -> java.io.File(dir, name).writeText(json) }
            dir
        }.onSuccess { dir ->
            Toast.makeText(this, "已导出到 ${dir.absolutePath}", Toast.LENGTH_LONG).show()
        }.onFailure { t ->
            Toast.makeText(
                this,
                "导出失败：${t.message ?: t.javaClass.simpleName}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    @SuppressLint("SetTextI18n")
    fun refresh(){
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager
                .getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        //获取APP版本versionName
        //获取APP版本versionName
        var versionName: String? = null
        if (packageInfo != null) {
            versionName = packageInfo.versionName
        }
        //获取APP版本versionCode
        //获取APP版本versionCode
        binding.version.text = getString(R.string.version) + versionName
        viewModel.refresh()
    }
    override fun initViewBinding(): ActivityAboutBinding {
        return ActivityAboutBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<AboutViewModel> {
        return AboutViewModel::class.java
    }

    private companion object {
        /** 导出插件诊断时选择目标目录的请求码。 */
        const val REQUEST_EXPORT_DIAGNOSTICS = 0x0D1A
    }
}
