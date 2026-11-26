## 版本说明

当前为插件化预览：内置插件不再随 APK 分发，官方整合包从 `oh-my-hita/registry` 安装。

> `eas/user/theta` 的 T2 页面仍在迁移，不能视为完整稳定版。

## 安装说明

- 包名 `io.github.kajimi16.ohmyhita`
- 与旧版同签名，可直接覆盖安装
- 首次启动为空壳，请从插件市场选择官方整合包

## 已知限制

- 晴课表的网页会话只能用于 Web 接口；导入课表需要先完成账号密码 API 登录。
- EAS 插件要求宿主 API 13；旧宿主会拒绝加载 `0.3.4`。
- `user`、`theta`、`resource` 的 T2 页面仍为 preview。
