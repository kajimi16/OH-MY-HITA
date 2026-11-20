# HITA_CUTE 0.9.0 集成记录

## 差异结论

`Zethpa/HITA_CUTE` 是 `LiPu-jpg/HITA_L` 1.5.2 的单体分支，版本 1.6.0；差异集中在：

- 自定义背景：图片/纯色、透明度、毛玻璃、裁剪/适应/拉伸。
- 20 套主题色，并可从背景图片提取最接近的主题色。
- 6 组课表配色：Material、Pastel、Macaron、Nature、Ocean、Sunset。
- 课表面板增加主题色/配色入口；科目颜色可继续单科覆盖。

它没有新的插件模块，因此不能作为独立市场 APK 直接安装。

## 集成映射

| HITA_CUTE 能力 | 8.1 实现 |
|---|---|
| 20 套主题色 | `CuteThemeStore` + `BaseActivity` 应用 `AppTheme_Cute*` |
| 课表配色 | `CutePaletteStore` + `ColorTools.applyPalette`，课表插件重置颜色时读取 |
| 背景/纯色 | `BackgroundRepository`、`BackgroundHelper`，应用在 HITA_CUTE 抽屉 |
| 提取主题色 | `DominantColorExtractor` + `CuteThemeStore.findClosest` |
| 外观入口 | HITA_CUTE 抽屉 → 可爱外观；非 CUTE 预设隐藏 |

## 预设

```json
{"id":"hita-cute-1.6.0","minHostApi":10,"plugins":[
  "com.stupidtree.hitax.timetable",
  "com.stupidtree.hitax.eas",
  "com.stupidtree.hitax.resource",
  "com.stupidtree.hitax.user",
  "com.stupidtree.hita.theta"
]}
```

默认 shell 与 HITA-L 一致，由 `MainActivity` 承载。`HITA_API_VERSION` 提升到 10，确保 0.8.0 无法应用这个依赖新宿主能力的预设。

## 限制

- CUTE 外观页是宿主 UI，不新增插件 API；插件不能直接修改主题/背景。
- 主题色切换通过 Activity 重建生效。
- 配色应用会重置最近课表的科目颜色；单科自定义颜色仍可覆盖。
