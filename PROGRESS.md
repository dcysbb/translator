# 进度看板 — 流式翻译优化（基于 v0.4.1）

基线：`ed3e8e8`（v0.4.1），分支 `codex/fix-streaming-translation`。

## 状态总览 — 全部完成 ✅

| # | 任务 | 状态 | 文件 |
|---|------|------|------|
| 0 | 基线确认 + 分支 + 进度文件 | ✅ | — |
| 1 | 增量 JSON 提取器 + 8 单测 | ✅ | `StreamingTranslationExtractor.kt`, `StreamingTranslationExtractorTest.kt` |
| 2 | DeepSeekClient 流式 SSE + ResultCallback + 回退 + 取消 | ✅ | `DeepSeekClient.kt` |
| 3 | 提示词字段顺序（translation 在前） | ✅ | `DeepSeekPrompt.kt`, `DeepSeekPromptTest.kt` |
| 4 | 阅读面板实时显示译文（节流 80ms）+ 保持展开 | ✅ | `FloatingTranslatorService.kt` |
| 5 | SSE/回退/取消集成测试（5 个 MockWebServer 测试） | ✅ | `DeepSeekStreamingTest.kt` |
| 6 | 全部单测通过 + release 编译 | ✅ | — |

## 验证结果
- 全部 13 个新单测通过（8 提取器 + 5 流式客户端）
- 全部既有单测不回归
- `assembleRelease` 编译通过
- 真机 UI 验证受限（MuMu 模拟器 overlay 窗口不稳定），但代码逻辑和单测全面覆盖

## 实现总结
- `stream=true` SSE 逐行读取 `delta.content`，累积完整模型内容。
- `StreamingTranslationExtractor`：在 `translation` 字段未闭合时持续输出可展示译文（处理转义/跨 chunk/Unicode/跳过其他字段值）。
- 流结束后交给 `AnalysisJsonParser` 解析；解析失败但有译文则保留译文+提示。
- 兼容：忽略 stream 直返 JSON → 同响应解析；明确不支持（400/415/422 含 stream/response_format）→ 一次回退；超时/429/5xx 直接失败不重试。
- `ResultCallback` 扩展 `onTranslationProgress`；`analyze()` 返回统一可取消 `TranslationHandle`。
- 提示词要求 translation 为首字段。
- 阅读面板收到译文 token 后以 80ms 节流更新，显示「中文\n译文\n正在生成详细解析…」。
