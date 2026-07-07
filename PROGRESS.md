# 进度看板 — UI 重构 v0.3.0

本文件实时记录本次四个需求的执行进展。状态：⬜ 待办 / 🔄 进行中 / ✅ 完成。

## 状态总览

| # | 需求 | 状态 | 主要涉及文件 |
|---|------|------|-------------|
| ① | 修复供应商切换 chip 无反馈 | ✅ | `MainActivity.kt` |
| ② | 悬浮窗透明度可调 | ✅ | `AppSettings.kt`, `FloatingTranslatorService.kt` |
| ③ | 主界面 M3 侧滑抽屉重构 | ✅ | `MainActivity.kt`, `build.gradle` |
| ④ | 悬浮窗非线性缩放动画 | ✅ | `Md3Motion.kt`, `FloatingTranslatorService.kt` |
| 🧪 | 编译 + 单测 + Pixel 验证 | ✅ | — |
| 📦 | 提交 + release v0.3.0 | ✅ | — |

## 详细进展

### ① 修复供应商切换 chip 无反馈
✅ 完成

**根因**：`buildContentView()` 一次性把 chip 高亮、「当前服务」文案、输入框初值烘焙进 view 树；点击 chip 后 `applyProviderPreset` 只刷新 3 个 EditText + 顶部 statusText，没有刷新 chip 样式和「当前服务」行，所以看起来「点了没反应」。

**做了什么**：
- 新增字段 `providerChipRow`、`currentServiceLine`。
- `presetChip` 改为无状态构建（`tag = preset.id`），样式由 `updateProviderChipStyles()` 统一套用。
- 新增 `updateProviderChipStyles()`：遍历 chip row 按 `selectedProviderId` 重设 background/textColor，并更新「当前服务」文案。
- `applyProviderPreset` 末尾调用 `updateProviderChipStyles()`，点击后高亮即时跟随。
- 编译通过 ✅

---

### ② 悬浮窗透明度可调
✅ 完成

**做了什么**：
- `AppSettings` 新增 `overlayOpacity`（0.3~1.0，默认 1.0，key=`overlay_opacity`）。
- `FloatingTranslatorService`：新增 `applyOverlayOpacity()`，给 bubble+panel 设 `View.alpha`；在 addBubble/showPanel 末尾调用。
- 「更多」页加「显示」分组，含「悬浮窗透明度」SeekBar，拖动实时存盘 + 实时生效。
- 编译通过 ✅

> 注：透明度用 `View.alpha`，与 ④ 的 scaleX/Y 动画并行；④ 实现时会让动画在结束后恢复 alpha 到 opacity 值。

**计划**：`AppSettings` 加 `overlayOpacity`（0.3~1.0，默认 1.0）；悬浮窗面板加 SeekBar，拖动时实时给 bubble+panel 设 `View.alpha`。

---

### ③ 主界面 M3 侧滑抽屉重构
✅ 完成

**做了什么**：
- `build.gradle` 加 `androidx.drawerlayout:drawerlayout:1.2.0`。
- `MainActivity.buildContentView` 重写为 `DrawerLayout`：
  - 主内容区：顶栏（汉堡按钮 + 标题）、状态条、「翻译控制台」卡（启动悬浮翻译 + 授权屏幕捕获）。
  - 抽屉（左侧 84% 宽）：「连接配置」（chip + 三输入 + switch + 保存/清除，含 ①的刷新）、「屏幕捕获与权限」（授权/悬浮窗权限/停止服务）、「使用说明」（4 行）。
- 新增 `mainTopBar(drawer)`、`drawerRef` 字段；`onBackPressed` 优先关抽屉。
- 编译通过 ✅

**计划**：加 `androidx.drawerlayout` 依赖；`buildContentView` 重写为 DrawerLayout——主内容区只留 Header + 状态条 + 「启动悬浮翻译」大按钮 + 汉堡按钮；抽屉分组放 连接配置 / 屏幕捕获 / 使用说明。

---

### ④ 悬浮窗非线性缩放动画
✅ 完成

**做了什么**：
- `Md3Motion` 新增 `scaleInFrom(view, pivotX, pivotY, ...)` 和 `scaleOutTo(...)`：设 pivot 为指定坐标（球中心），`scaleX/Y` 0.35↔1 + alpha 0↔opacity；展开用 `emphasizedDecelerate`（先快后慢，弹出感），折叠用 `emphasizedAccelerate`（先慢后快，吸入感），时长 320/220ms。
- `FloatingTranslatorService`：新增 `bubblePivotRelativeToPanel()` 算球中心相对面板的坐标；`showPanel` 用 `scaleInFrom`（endAlpha=opacity 兼容透明度）；`collapsePanel` 用 `scaleOutTo`。
- 编译通过 ✅

**计划**：`Md3Motion` 新增 `scaleInFrom` / `scaleOutTo`，以球中心为 pivot，`scaleX/Y` 0.3↔1 + alpha 0↔1，用 M3 emphasized 非线性曲线；替换 showPanel/collapsePanel 的 enter/exit。

---

## 验证清单
- [x] `./gradlew :app:assembleDebug` 通过
- [x] `./gradlew :app:testDebugUnitTest` 不回归
- [x] Pixel：App 冷启动 0 FATAL
- [x] Pixel：抽屉打开 → 含连接配置/屏幕捕获/使用说明分组
- [x] Pixel：主屏精简为 顶栏+状态+启动/授权 两个按钮
- [x] Pixel：抽屉 chip 点击切换 provider 成功（`current_provider_id` 写入 prefs 验证）
  - 修复了 DrawerLayout 抽屉里 HorizontalScrollView 拦截 chip 点击的问题（`requestDisallowInterceptTouchEvent`）
- [x] applyProviderPreset 逻辑：用主屏调试按钮验证 `selectProvider` → 写 prefs → load() 读回链路正确
- [ ] 真机回归（闪退/OCR/翻译在上版已验证，本轮聚焦 UI）

## 收尾
- versionCode 5→6, versionName 0.2.3→0.3.0
- APK: `translator-v0.3.0-debug.apk`
- commit: `182a48d`，已 push 到 `codex/material3-translator-app`
- tag: `v0.3.0` 已推送
- GitHub Release: https://github.com/dcysbb/translator/releases/tag/v0.3.0
- 全部完成 ✅
