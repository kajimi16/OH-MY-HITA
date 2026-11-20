# 强插件化迁移报告

日期：2026-11-20

## 已完成

- 17 个原有 UI 页面拆成 `oh-my-hita/ui-*` 仓库，统一依赖 `plugin-sdk`，有 `v*` 发布 workflow。
- 新增 `oh-my-hita/ui-timetable`：T2 课表 **preview**，只调用 `TimetableService`，不访问 Room/DAO/ICS；当前只渲染当前周事件列表，不能计入完整 T2 UI 验收。
- 新增 `oh-my-hita/timetable-core`：T1 课表核心，Room/DAO/仓库/ICS/同步 codec；发布 v0.1.0 dex。
- 新增 `oh-my-hita/resource-core` 与 `oh-my-hita/ui-resource`：T1 HOA/教师数据 + T2 搜索/Readme preview，均已发布 v0.1.0。
- 新增 API 11 的 `HostEnvironmentService`，只暴露 `applicationContext`；T1 插件用无参 `Plugin` 装载。
- 修复 `:api:generateApiAbi` 误取过期 release jar 的问题；ABI 现在覆盖 78 个类。
- `plugin-sdk:11` 已发布；T1 core 使用远程坐标构建。
- `registry` 已包含 18 个 UI 插件、T1 课表核心与三套官方整合包声明。
- 空壳宿主已验证能启动、拉取 registry 并展示 UI 插件。

## 仍未完成

| 项目 | 当前状态 |
|---|---|
| `eas/user/theta` 官方仓库 | 仍未拆出，仍走 privileged builtin |
| `resource` | T1/T2 已拆出；T2 仍是搜索/Readme preview，不是完整资源页 |
| `timetable` 完整 T2 UI | **未完成**；当前 v0.1.0 只是 preview，只有当前周事件列表，周网格、编辑、详情未移植 |
| `timetable` T3 | Widget、提醒、前台服务仍在宿主 |
| 官方整合包 1.0 | 三套 preset 仍引用 5 个 builtin，不能宣称完全空壳化 |
| 发布线 | 最新 release 是 0.9.0（API 10）；API 11 只在 master |

## 关键风险

1. T1 与 T2 各自能构建，但还没有在官方 preset 中同时启用；只安装 T2 会显示“未安装 T1”。
2. `ui-timetable` 的 `mainTabs` 与宿主 builtin 课表 tab 使用同一入口；在 builtin 仍存在时只能作为调试/替换页，不能直接叠加。
3. API 11 使 `requiresApi=11` 的插件无法安装到 0.9.0；发布 0.10/1.0 前需要决定兼容策略。
4. 官方 preset 的 `builtin` 引用是显式保留，不是 schema 占位；删除前必须先有对应 T2/T3。
5. T1 dex 与 T2 APK 的发布流程已独立；但 T1 的启动恢复、禁用与 kill switch 仍未在官方 preset 中验证。

## 下一步顺序

1. `resource`：把 `ResourceDataServiceImpl`、`TeacherServiceImpl` 抽成 T1；T2 只保留页面；T3 负责 WebView/文件选择。
2. `user`：用现有 `user` 模块做 T1，补 `UserSessionService`；`plugin-user` 页面改 T2。
3. `eas`：抽 `EASRepository` 与登录态；T2 页面；Worker/登录 Activity 最后留 T3。
4. `theta`：抽仓库/网络；T2 话题与文章；分享/深链留 T3。
5. `timetable`：补完整周视图，再删除宿主 builtin 入口。
6. 五者都达到 `T2_READY` 后，才发布官方整合包 1.0。

## 验证命令

```bash
# T1
cd ../timetable-core && ./gradlew :plugin:buildPluginDex
# T2
cd ../ui-timetable && ./gradlew :ui-plugin:assembleDebug
# 宿主
./gradlew :app:assembleDebug
# 空壳
./gradlew :app:assembleDebug -PpluginIndexUrl=<registry>
```

## 结论

架构方向已经验证：Android 上可行的不是 Cordis 式“所有东西动态卸载”，而是 T1 逻辑核心、T2 Compose 界面、T3 宿主组件的分层。当前只完成了 timetable 的 T1 core 与 T2 preview 样本，不能把完整 T2 UI 或整个迁移标记为完成。
