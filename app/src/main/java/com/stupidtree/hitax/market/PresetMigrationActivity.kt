package com.stupidtree.hitax.market

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.HitaServices
import com.stupidtree.hitax.utils.ActivityUtils
import com.stupidtree.hitax.utils.IcsImportUtils

/**
 * 预设数据迁移：只接受用户在旧 HITA 中主动导出的 ICS。
 * 不读取其它应用的私有数据库。
 */
class PresetMigrationActivity : AppCompatActivity() {

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importIcs(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 32)
            setBackgroundColor(Color.TRANSPARENT)
        }
        root.addView(TextView(this).apply {
            text = "迁移课表"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "在 HITA_Android 中打开课表导出，保存 ICS 文件后在这里选择。\n\n" +
                "成绩、教室缓存和 AI 会话不会迁移，请在新版重新登录 / 拉取。"
            textSize = 15f
            setPadding(0, 24, 0, 24)
        })
        root.addView(Button(this).apply {
            text = "选择 ICS 文件"
            setOnClickListener {
                picker.launch(IcsImportUtils.pickerMimeTypes())
            }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        setContentView(root)
    }

    private fun importIcs(uri: android.net.Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val stream = runCatching { contentResolver.openInputStream(uri) }.getOrNull()
        if (stream == null) {
            Toast.makeText(this, "无法读取所选 ICS 文件", Toast.LENGTH_SHORT).show()
            return
        }
        HitaServices.timetable(application).importFromICSAsNewTimetable(
            stream,
            IcsImportUtils.getDisplayName(this, uri),
        ).observe(this) { result ->
            when (result.state) {
                DataState.STATE.SUCCESS -> {
                    val data = result.data ?: return@observe
                    Toast.makeText(
                        this,
                        "已创建课表“${data.timetableName}”，导入 ${data.importedCount} 个课程",
                        Toast.LENGTH_LONG,
                    ).show()
                    ActivityUtils.startTimetableDetailActivity(this, data.timetableId)
                    finish()
                }
                DataState.STATE.FETCH_FAILED -> {
                    Toast.makeText(this, "导入失败：${result.message}", Toast.LENGTH_LONG).show()
                }
                else -> Toast.makeText(this, "导入失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
