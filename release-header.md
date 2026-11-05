## 版本说明

- **自定义插件指北**：插件市场新增第三栏，用 MG 动画和步骤说明展示
  「编写插件 → 推送仓库 → CI 构建 → 更新索引 → 市场安装」的完整流程。
- **中央插件索引**：默认插件源切换到社区仓库 `hita-community-plugins` 的
  `registry/index.json`；`market` 分支的 `market/index.json` 继续作为可替换源。
- **发布流水线**：
  - `Publish Plugin` 工作流按仓库、模块与版本构建 APK/dex
  - 构建 job 只读、无持久化凭据；发布 job 单独持有写权限
  - 界面插件会校验 `assets/plugin.json` 的 id / version / entryClass / apiVersion
  - 计算 SHA-256 后写入 `dist/` 与中央索引
- **契约 AAR**：模板与社区仓库的 `api/model/component` AAR 已刷新到 API 5，
  外部插件构建时会注入最新 AAR。
- **市场**：继续提供「查询并下载 / 我的插件」两栏，支持自定义源、离线缓存、
  按来源更新与全部更新。
- **安全**：插件包做 SHA-256 校验；插件与宿主同进程，请只安装可信来源。

### 已知差异

- 底栏为自绘动态栏，视觉与原版略有差异
- 课程资源「投稿」页仍为内置实现
- 界面插件版 README 暂未处理相对链接与 HTML 表格，渲染约 1% 差异
- 插件索引尚未做签名校验，发布审核仍由维护者人工完成

### 安装

- 包名 `io.github.kajimi16.ohmyhita`，可与原版 HITA 共存
- 同包名可直接覆盖升级（签名不变），从 0.4.6 升级会保留数据
