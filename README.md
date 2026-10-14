# MmsClean

MmsClean 是一个基于 **libxposed API 102** 的 ColorOS
信息（短信）净化模块，用于移除输入框上方菜单和消息卡片中的营销按钮，同时保留常用功能类操作。模块没有独立用户界面，启用后常驻生效。

## 功能

| 功能                 | 处理方式                                     |
|----------------------|----------------------------------------------|
| 移除输入框上方菜单   | 将服务端下发的菜单列表清空                   |
| 移除消息卡片营销按钮 | 从动作列表中剔除指定营销动作，保留功能类动作 |

当前过滤的动作类型如下：

| `action` 值 | 行为                 |
|-------------|----------------------|
| `3`、`4`    | 打开网页             |
| `6`         | 打开应用             |
| `12`        | 快递查询并跳转浏览器 |
| `23`        | 中国移动营销         |

拨号（`8`）、邮件（`9`）、地图（`10`）、复制验证码（`16`）、复制关键词（`18`）等功能类动作会被保留。需要调整过滤范围时，修改
`MmsCleanHook.java` 中的 `MARKETING_ACTIONS` 集合即可。

## 兼容性与安装

| 项目        | 要求                                 |
|-------------|--------------------------------------|
| Android     | Android 12 及以上（`minSdk 31`）     |
| Xposed 框架 | 支持 libxposed API 101 及以上的框架  |
| 目标应用    | ColorOS 信息，包名 `com.android.mms` |
| 已验证版本  | `17.100.0`（`versionCode 17100000`） |

安装步骤：

1.  安装 APK。
2.  在兼容的 Xposed
    框架中启用模块，并将「信息」（`com.android.mms`）加入作用域。
3.  重启「信息」应用。

模块没有独立设置界面，两个功能均默认启用。

## 构建

构建环境：

- JDK 17
- Gradle 8.7
- Android Gradle Plugin（AGP）8.5.2
- Android `compileSdk 34`

在项目根目录执行：

``` bash
./gradlew :app:assembleRelease
```

构建产物默认位于：

``` text
app/build/outputs/apk/release/app-release.apk
```

仓库使用 debug 签名配置，构建产物可直接安装使用。如需以自己的密钥签名，请在
`app/build.gradle` 中配置 `signingConfigs`。

## 实现说明

| 功能             | Hook 点                                                                               | 处理逻辑                                     |
|------------------|---------------------------------------------------------------------------------------|----------------------------------------------|
| 输入框上方菜单   | `com.opos.smart.mms.interfaces.netmsg.menu.MenuInfoBaseBean#getMenus()`               | 拦截方法并返回空列表                         |
| 消息卡片营销按钮 | `com.ted.android.data.bubbleAction.ActionBase#fromJSONArray(BubbleEntity, JSONArray)` | 调用原方法，再从返回的动作列表中过滤指定类型 |

消息卡片动作通过 `action`
字段的整型值进行分类。模块仅移除配置的营销动作类型，其他动作保持原样。过滤集合定义在
`MmsCleanHook.java` 的 `MARKETING_ACTIONS` 中。

部分 Hook 思路和营销动作类型参考了
[LuckyTool](https://github.com/luckyzyx/LuckyTool)（作者：luckyzyx）。输入框菜单
Hook 点与 LuckyTool 一致；营销动作类型 `3`、`4`、`6`、`12`、`23` 参考其
`SKIP_ACTION` 表。动作类型对应的实际行为则根据 ColorOS 信息 `17.100.0`
的字节码独立核对。

消息卡片营销按钮的 Hook 点由本模块自行定位，与 LuckyTool
的实现不同。LuckyTool 相关实现通过拦截 `org.json.JSONObject(String)`
构造器，再从卡片级 JSON 中读取 `entities` 数组。经本模块对 ColorOS 信息
`17.100.0` 的实测，`entities` 和 `actions` 均以嵌套 JSON
字符串形式提供，使用 `optJSONArray("entities")` 会返回
`null`，因此该方式在此版本中不生效。本模块改为在
`ActionBase#fromJSONArray(BubbleEntity, JSONArray)`
返回动作列表后进行过滤。

本模块使用 libxposed API 102；LuckyTool 的相关实现使用
YukiHookAPI（legacy API 82）。

## 已知限制

如果功能未生效，请检查模块是否启用、「信息」是否加入作用域、目标应用是否已重启，以及当前应用版本是否与已验证版本一致。

- 仅适用于 ColorOS 信息，不适用于第三方短信应用。
- 依赖 ColorOS
  私有类和方法；应用更新后，类名、方法签名或数据结构可能变化，需要重新适配。
- 不适用于仅支持 legacy API 82 的旧框架。

## 许可证与免责声明

本模块以 **GNU General Public License v3.0** 发布，许可证全文见 [LICENSE](LICENSE)。
部分实现思路参考了同样采用 GPL-3.0 的 [LuckyTool](https://github.com/luckyzyx/LuckyTool)。

本模块仅供个人学习与自用。使用本模块产生的任何后果由使用者自行承担。
