# 文档索引

本目录区分“当前有效文档”和“历史记录”。历史文档保留当时的版本、commit 和截图，
不应直接当作当前发布状态。

## 当前有效

| 文档 | 内容 |
|---|---|
| `../README.md` | 项目简介、下载、安装、当前版本与已知限制 |
| `../API清单.md` | 教务接口调研记录（部分为历史手工验证，不是稳定契约） |
| `porting-status.md` | 当前移植状态与历史验收摘要 |
| `api/plugin-api.md` | 插件 API 契约 |
| `api/timetable-appearance.md` | 课表外观 API |
| `api/usage.md` | 使用统计与市场人数接口 |
| `plugin-roadmap.md` | 微内核/插件体系设计路线图（部分已完成） |
| `plans/2026-11-20-0.8.0-motion-sound-dependencies.md` | 0.8.0 动效/音效/插件依赖调研 |
| `plans/2026-11-20-customization-gaps.md` | 当前不可自定义能力与优先级 |
| `plans/2026-11-20-customization-api-expansion.md` | 自定义能力 API 扩展方案 |
| `plans/2026-11-20-plugin-market-redesign.md` | 0.8.0 插件市场信息架构与 UI/UX 改版设计 |
| `plans/2026-11-20-hita-cute-integration.md` | 0.9.0 HITA_CUTE 差异与集成映射 |

## 历史计划

`plans/` 下文档按日期保留，记录当时的目标、方案、提交和验收结果：

- `2026-03-13-*`：ICS 分享、版本清理、学期名缩短
- `2026-11-04-timetable-plugin-extraction.md`：课表插件拆分
- `2026-11-18-7.1-ui-alignment-and-bugs.md`：7.1 UI 对齐与已接受残差
- `2026-11-18-plugin-remaining-work.md`：插件剩余工作
- `2026-11-19-0.7.2-*`：市场分区、交互恢复、HITA-L 复刻
- `2026-11-19-0.7.3-*`：整合包切换、市场卡片、今日/更多页
- `2026-11-19-new-page-ui-review.md`、`2026-11-19-pixel-audit.md`：UI/像素复审
- `2026-11-19-plugin-chrome-analytics-catalog.md`：插件 Chrome、统计、课程池
- `2026-11-19-timetable-more-parity-design.md`：时间表/更多页对齐

## 历史证据

`evidence/` 是旧仓库时期的像素与状态证据，包含旧版本截图和 manifest。
它用于追溯当时的验收，不代表新独立仓库的当前发布结果。说明见
`evidence/README.md`。

## 时效性规则

1. 当前版本、下载链接以根 `README.md` 和 GitHub Releases 为准。
2. 计划文档中的 commit、tag、分支名是历史记录，不保证仍可解析。
3. 证据文档中的版本号、截图和差异比例是当时快照，除非复审记录明确更新。
4. 新仓库迁移后的状态以 `porting-status.md` 顶部“当前仓库”段落为准。
