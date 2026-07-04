# 屏幕翻译 v1.4

修复一个会在刷新翻译时误显示技术性异常的 bug。

## 本次更新（v1.4）
- **修复 "StandaloneCoroutine was canceled" 误显示**：
  - **根因**：连点刷新时，`CaptureService` 会取消上一个未完成的翻译请求，而 Kotlin 协程的取消信号（`CancellationException`）被 `DeepSeekClient.translate` 的 `catch (e: Exception)` 错误吞掉，当成翻译失败返回，于是面板显示 "StandaloneCoroutine was canceled"。
  - **修复**：在 catch 链中单独捕获并重新抛出 `CancellationException`，让协程取消作为正常控制流传播；只有真正的网络/HTTP 错误才转为错误结果。现在连点刷新时，旧请求会静默终止，只显示最新一次的结果。
- **新增回归测试** `cancellationPropagatesInsteadOfBecomingError`：模拟慢响应中途取消，断言抛出 `CancellationException` 而非返回错误结果，锁死此 bug 不再回归。

## 升级说明
从 v1.3 升级的用户：刷新翻译时不再误报 "StandaloneCoroutine was canceled"，其余行为不变。

## 已知限制（MVP）
- 选区固定为屏幕上半幅，暂未做拖拽选框交互。
- 仅 Wi-Fi 开关已持久化，尚未在请求链路真正拦截。
- 仅 debug 构建（未签名），仅供本机测试安装。

> ⚠️ 这是 debug APK，未做正式签名，安装时可能需要允许「未知来源」。
