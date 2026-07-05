# codex 版本 v0.1.0 参考存档

本分支 `codex-v0.1.0-reference` 存档的是 **codex 最初生成的屏幕翻译应用**
(`com.poozh.translator`, versionCode 1 / versionName **0.1.0**),供参考与功能移植。

> ⚠️ 这是 **反编译产物**,不是原始源码。原始 Kotlin 源码已丢失,这里的内容是用
> jadx 1.5.1 从 debug APK 反编译得到的 Java。变量名、控制流基本忠实,但 Kotlin
> 特性(coroutine、密封类、属性委托等)会被还原成较啰嗦的 Java 写法,且含
> `Intrinsics.checkNotNullParameter` 等编译器插入的代码。**移植时按语义理解、用
> Kotlin 重写,不要照抄。**

## 来源

- **原始 APK**:`apk/app-debug-codex-v0.1.0.apk`
  (SHA-256 `ce6889b7c656ed38b192a9cc51b693ccd3bfc6a4d606b0c513303ee4fb210a6a`)
- **包名 / 版本**:`com.poozh.translator` / 0.1.0
- **应用名**:屏幕翻译
- **SDK**:minSdk 26, targetSdk/compileSdk 36
- **反编译**:jadx 1.5.1 (`--no-res`)
- **反编译时间**:2026-07-05

## 目录结构

```
codex-v0.1.0/
├── apk/
│   └── app-debug-codex-v0.1.0.apk      # 原始 APK 存档
├── manifest/
│   └── AndroidManifest.xml.txt         # aapt2 dump 的可读 Manifest
├── sources/com/poozh/translator/        # 反编译业务源码(23 个 .java / 4567 行)
│   ├── MainActivity.java                # 主界面(API Key 设置等)
│   ├── FloatingTranslatorService.java   # 悬浮服务(1276 行,核心)
│   ├── R.java
│   ├── capture/ScreenCaptureController  # MediaProjection 截屏 + 按选区裁剪
│   ├── ocr/ScreenTextRecognizer         # ML Kit 文字识别
│   ├── data/
│   │   ├── DeepSeekClient               # DeepSeek 翻译/语法分析 API
│   │   ├── DeepSeekPrompt               # 提示词模板
│   │   ├── AnalysisJsonParser           # 结构化结果解析
│   │   ├── AppSettings / SettingsSnapshot
│   │   └── OverlayWindowState
│   ├── model/
│   │   ├── AnalysisResult / TermNote    # 语法分析结构化模型
│   │   ├── LanguageDetector / TextLanguage  # 语言检测
│   │   ├── TranslationRefreshPolicy / RefreshAction  # 刷新去抖策略
│   └── ui/
│       └── SelectionOverlayView         # ★ 画框选区(全屏遮罩 + 拖拽选区)
└── README.md
```

## 与当前 master (`com.example.translator`, v1.6) 的关键差异

codex v0.1.0 在若干功能上 **比当前主分支更完整**:

| 功能 | codex v0.1.0 | 当前 master v1.6 |
|------|--------------|------------------|
| **画框选区** | ✅ `ui/SelectionOverlayView` — 全屏半透明遮罩,拖拽画框,抬手判定最小尺寸后回调 `Rect`,存入 `selectionRect` 供截屏裁剪 | ❌ 写死取屏幕上半屏,无画框 UI |
| **语言检测** | ✅ `model/LanguageDetector` + `TextLanguage` | ❌ 无 |
| **语法分析** | ✅ `AnalysisResult` / `TermNote` 结构化模型 + `AnalysisJsonParser` | ⚠️ 仅有简单解析 |
| **刷新策略** | ✅ `TranslationRefreshPolicy` / `RefreshAction` | ⚠️ 仅有 `OcrTextCache` 去重 |
| 悬浮窗实现 | 原生 View(`FloatingTranslatorService`,单文件 1276 行) | Jetpack Compose 双窗口 |
| 悬浮窗拖动 | 原生触摸 | 原生 + Compose 拖动手柄 |
| 闪烁问题 | (未实测) | ✅ v1.6 双窗口已彻底修复 |

### 移植优先级建议(若要把缺失功能搬到 master)

1. **画框选区**(`ui/SelectionOverlayView`)—— 用户明确反馈过"无法画框",
   这是最高价值移植项。集成点见 `FloatingTranslatorService.showSelectionOverlay()`
   (约第 613 行):刷新时若 `selectionRect == null` 则弹出选区视图,画框回调里
   存 rect 并传给截屏控制器裁剪。移植到 Compose 时可保留原生 View 实现,作为
   独立全屏窗口 addView。
2. **语言检测**(`model/LanguageDetector`)—— 纯逻辑,无 UI 依赖,容易搬。
3. **语法分析结构化**(`model/AnalysisResult` + `data/AnalysisJsonParser`)——
   提升翻译结果展示质量。
4. **刷新策略**(`TranslationRefreshPolicy`)—— 替换简陋的 `OcrTextCache`。

## 注意事项

- 反编译代码仅供 **参考语义**,不可直接编译(缺 R 资源、含 jadx 残留语法)。
- 移植时以当前 master 的 Compose 架构为准,把 codex 的 **逻辑** 用 Kotlin 重写。
- 原始 APK 可用 `aapt2 dump badging` / jadx 重新反编译核对。
