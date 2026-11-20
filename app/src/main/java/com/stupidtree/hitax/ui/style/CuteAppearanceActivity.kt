package com.stupidtree.hitax.ui.style

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.R
import com.stupidtree.hitax.data.repository.BackgroundRepository
import com.stupidtree.hitax.data.repository.BackgroundSettings
import com.stupidtree.hitax.databinding.ActivityCuteAppearanceBinding
import com.stupidtree.hitax.utils.BackgroundHelper
import com.stupidtree.hitax.utils.DominantColorExtractor
import com.stupidtree.style.CutePaletteStore
import com.stupidtree.style.CuteThemeStore
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.widgets.PopUpColorPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** HITA_CUTE 外观设置：主题色、课表配色、背景图和背景色。 */
class CuteAppearanceActivity :
    BaseActivity<CuteAppearanceViewModel, ActivityCuteAppearanceBinding>() {

    private val backgroundRepository by lazy {
        BackgroundRepository.getInstance(application)
    }
    private var localSettings = BackgroundSettings()
    private var localMode = "image"

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val copied = withContext(Dispatchers.IO) {
                BackgroundHelper.copyToInternalStorage(application, uri)
            }
            if (copied.isBlank()) {
                toast("无法读取图片")
                return@launch
            }
            localSettings = localSettings.copy(imageUri = copied)
            localMode = "image"
            updateBackgroundPreview()
        }
    }

    override fun initViewBinding(): ActivityCuteAppearanceBinding =
        ActivityCuteAppearanceBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<CuteAppearanceViewModel> = CuteAppearanceViewModel::class.java

    override fun initViews() {
        binding.cuteToolbar.setNavigationOnClickListener { finish() }
        localSettings = backgroundRepository.getSettingsSnapshot()
        localMode = if (localSettings.bgColor.isNotBlank() && localSettings.imageUri.isBlank()) "color" else "image"

        binding.backgroundEnable.isChecked = localSettings.enabled
        binding.backgroundTransparency.progress = localSettings.transparency
        binding.backgroundBlur.progress = localSettings.blurRadius
        binding.backgroundFit.check(
            when (localSettings.fitMode) {
                "fit" -> R.id.fit_fit
                "stretch" -> R.id.fit_stretch
                else -> R.id.fit_crop
            },
        )
        binding.backgroundPreview.setOnClickListener { pickImage.launch(arrayOf("image/*")) }
        binding.backgroundPick.setOnClickListener { pickImage.launch(arrayOf("image/*")) }
        binding.backgroundColorPick.setOnClickListener { pickColor() }

        binding.backgroundTransparency.setOnSeekBarChangeListener(simpleSeek { value ->
            localSettings = localSettings.copy(transparency = value)
        })
        binding.backgroundBlur.setOnSeekBarChangeListener(simpleSeek { value ->
            localSettings = localSettings.copy(blurRadius = value)
        })
        binding.backgroundApply.setOnClickListener { applyBackground() }
        binding.backgroundRemove.setOnClickListener {
            backgroundRepository.setImageUri("")
            backgroundRepository.setBgColor("")
            backgroundRepository.setEnabled(false)
            BackgroundHelper.clearCache()
            finish()
        }
        binding.autoTheme.setOnClickListener { extractThemeFromBackground() }

        renderThemeGrid()
        renderPaletteGrid()
        updateBackgroundPreview()
    }

    private fun renderThemeGrid() {
        binding.themeGrid.removeAllViews()
        val active = CuteThemeStore.activeId(this)
        CuteThemeStore.options.forEach { option ->
            val tile = FrameLayout(this).apply {
                foreground = getDrawable(android.R.drawable.list_selector_background)
                contentDescription = option.label
                setOnClickListener {
                    CuteThemeStore.setActiveId(this@CuteAppearanceActivity, option.id)
                    recreate()
                }
            }
            val color = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(option.colorHex))
                    setStroke(
                        if (option.id == active) dp(3) else dp(1),
                        if (option.id == active) Color.BLACK else Color.TRANSPARENT,
                    )
                }
            }
            tile.addView(color, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER))
            val params = android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = dp(58)
                columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
            }
            binding.themeGrid.addView(tile, params)
        }
    }

    private fun renderPaletteGrid() {
        binding.paletteGrid.removeAllViews()
        val active = CutePaletteStore.activeId(this)
        CutePaletteStore.options.forEach { palette ->
            val card = MaterialCardView(this).apply {
                radius = dp(12).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(if (palette.id == active) 0xFFEDF2FF.toInt() else 0xFFFFFFFF.toInt())
                setOnClickListener {
                    CutePaletteStore.setActiveId(this@CuteAppearanceActivity, palette.id)
                    applyPaletteToTimetable()
                    renderPaletteGrid()
                }
            }
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            val label = TextView(this).apply {
                text = palette.label + if (palette.id == active) "  ✓" else ""
                setTextColor(0xFF111827.toInt())
                textSize = 14f
            }
            val colors = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8), 0, 0)
            }
            palette.colors.forEach { hex ->
                val dot = View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(hex))
                    }
                }
                colors.addView(dot, LinearLayout.LayoutParams(dp(12), dp(12)).apply {
                    marginEnd = dp(2)
                })
            }
            content.addView(label)
            content.addView(colors)
            card.addView(content)
            val params = android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = dp(72)
                columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            binding.paletteGrid.addView(card, params)
        }
    }

    private fun applyPaletteToTimetable() {
        val service = (application as? HApplication)?.pluginHost?.service(TimetableService.KEY)
        if (service == null) {
            toast("课表插件不可用，配色将在下次导入时生效")
            return
        }
        service.actionResetRecentSubjectColors()
        toast("课表配色已应用")
    }

    private fun pickColor() {
        val initial = runCatching { Color.parseColor(localSettings.bgColor) }
            .getOrDefault(Color.parseColor("#B0BEC5"))
        PopUpColorPicker()
            .initColor(initial)
            .setOnColorSelectListener(object : PopUpColorPicker.OnColorSelectedListener {
                override fun onSelected(color: Int) {
                    localSettings = localSettings.copy(bgColor = String.format("#%06X", 0xFFFFFF and color))
                    localMode = "color"
                    updateBackgroundPreview()
                }
            })
            .show(supportFragmentManager, "cute_bg_color")
    }

    private fun updateBackgroundPreview() {
        binding.backgroundPreview.visibility = View.GONE
        binding.backgroundColorPreview.visibility = View.GONE
        if (localMode == "color") {
            binding.backgroundColorPreview.visibility = View.VISIBLE
            binding.backgroundColorPreview.setBackgroundColor(
                runCatching { Color.parseColor(localSettings.bgColor) }.getOrDefault(Color.LTGRAY),
            )
        } else {
            val uri = localSettings.imageUri
            if (uri.isNotBlank()) {
                binding.backgroundPreview.visibility = View.VISIBLE
                Glide.with(this).load(uri.removePrefix("file://")).centerCrop().into(binding.backgroundPreview)
            }
        }
    }

    private fun applyBackground() {
        if (localMode == "color") {
            backgroundRepository.setImageUri("")
            backgroundRepository.setBgColor(localSettings.bgColor)
        } else {
            backgroundRepository.setBgColor("")
            backgroundRepository.setImageUri(localSettings.imageUri)
        }
        backgroundRepository.setEnabled(binding.backgroundEnable.isChecked)
        backgroundRepository.setTransparency(binding.backgroundTransparency.progress)
        backgroundRepository.setBlurRadius(binding.backgroundBlur.progress)
        backgroundRepository.setFitMode(selectedFitMode())
        BackgroundHelper.clearCache()
        toast("背景已更新")
        setResult(RESULT_OK)
        finish()
    }

    private fun extractThemeFromBackground() {
        val settings = backgroundRepository.getSettingsSnapshot()
        if (settings.imageUri.isBlank()) {
            toast("请先选择背景图片")
            return
        }
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                BackgroundHelper.getProcessedBitmap(settings.imageUri, 0, 300, 300)
            }
            if (bitmap == null) {
                toast("无法读取背景图片")
                return@launch
            }
            val closest = CuteThemeStore.findClosest(DominantColorExtractor.extract(bitmap))
            bitmap.recycle()
            CuteThemeStore.setActiveId(this@CuteAppearanceActivity, closest.id)
            toast("已选择最接近的主题色：${closest.label}")
            recreate()
        }
    }

    private fun selectedFitMode(): String = when (binding.backgroundFit.checkedButtonId) {
        R.id.fit_fit -> "fit"
        R.id.fit_stretch -> "stretch"
        else -> "crop"
    }

    private fun simpleSeek(onChange: (Int) -> Unit) =
        object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onChange(progress)
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

class CuteAppearanceViewModel : ViewModel()
