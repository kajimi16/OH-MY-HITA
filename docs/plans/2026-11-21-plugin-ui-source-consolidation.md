# 主仓库 UI 插件源码收口方案

日期：2026-11-21
目标：18 个已发布 UI 插件以 `oh-my-hita/ui-*` 为唯一源码，主仓库只保留宿主、核心插件与示例模板。

## 1. 前置迁移（已完成）

| 项 | 结果 |
|---|---|
| `ui-compare` 测试 | `CompareLogicTest` 2/2 通过，提交 `6ce3e08` |
| `ui-homework` 测试 | `HomeworkDateTest` 3/3 通过，提交 `3cdbf44` |
| `ui-notice` 测试 | `NoticeParserTest` 6/6、`NoticeFetcherTest` 2/2，HTML fixture 已迁，提交 `3aae0ae` |
| `ui-share` 测试 | `ShareCodecTest` 2/2 通过，提交 `2180bd8` |
| `ui-resource` 0.4.8 源码 | 已同步为 `v0.4.10`，构建/设备打开成功，恢复 `mainTabs`；Release 已发布 |
| registry | `ui-resource` 为 `0.4.10`、`composeVersion 1.9.4`、下载地址 `v0.4.10/plugin.apk` |

## 2. 删除范围

删除 18 个目录：

```
plugin-ui-ai            plugin-ui-announcement   plugin-ui-appearance
plugin-ui-community     plugin-ui-compare        plugin-ui-credit
plugin-ui-group         plugin-ui-homework       plugin-ui-links
plugin-ui-live          plugin-ui-news           plugin-ui-notice
plugin-ui-reminder      plugin-ui-resource       plugin-ui-score
plugin-ui-share         plugin-ui-training       plugin-ui-update
```

保留：

- `plugin-ui-sample`：第三方插件模板。
- `plugin-apk-sample`：独立 APK 示例。
- `plugin-hita`、`plugin-eas`、`plugin-resource`、`plugin-user`、`theta`：核心逻辑插件。
- 宿主内置 UI/T3 代码：`app` 与核心模块。

## 3. 依赖与修改点

| 文件 | 修改 | 所有者 |
|---|---|---|
| `settings.gradle` | 从 `include` 移除 18 个模块，保留 sample | 集成 worker |
| `scripts/dev-install-ui-plugin.sh` | 参数改为 APK 路径，不再按模块名构建 | 集成 worker |
| `README.md` | 开发安装命令改为 APK 路径；注明源码在 `oh-my-hita/ui-*` | 集成 worker |
| `app/build.gradle` | 无需改；`plugin-ui-` 分支只服务保留的 sample | 不改 |
| `docs/plans/*`、`docs/porting-status.md` | 历史记录，不改写 | 不改 |
| 18 个目录 | 删除 | 三个删除 worker 各 6 个 |
| `NOTICE`、`licenses/hita1-share/` | 保留 HITA1 分享协议署名，避免删除 share 后引用悬空 | 集成 worker |

脚本新接口：

```bash
./scripts/dev-install-ui-plugin.sh /path/to/plugin-debug.apk
```

脚本校验 APK 存在，从 `assets/plugin.json` 读取 `id/version/entryClass`，沿用现有广播安装与 sha256 校验。

## 4. 并行切片

- `SwarmDelA`：ai、announcement、appearance、community、compare、credit。
- `SwarmDelB`：group、homework、links、live、news、notice。
- `SwarmDelC`：reminder、resource、score、share、training、update。
- `SwarmIntegrate`：settings、脚本、README；不碰删除目录。
- `SwarmVerify`：只读复核，跑最终验收命令。

删除 worker 只执行 `rm -rf` 对应目录，不修改 settings/脚本/README。

## 5. 验收

```bash
./gradlew :app:assembleDebug
./gradlew :api:verifyApiAbi :app:verifyPluginBoundary
./gradlew :app:testDebugUnitTest
```

附加检查：

- `settings.gradle` 不再 include 18 个已发布模块。
- `grep -R "plugin-ui-" --exclude-dir=build --exclude-dir=.git` 只剩 sample、脚本和文档历史引用。
- 18 个目录不存在。
- 从 registry 下载 `ui-resource 0.4.10` 安装并打开；preset 的资源 tab 解析为 `PLUGIN`。
- 外部 4 个测试仓库 `testDebugUnitTest` 仍通过。

## 6. 回滚

删除提交可整体 revert；独立仓库的源码、测试与 Release 已保留，不依赖主仓库副本。
