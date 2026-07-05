package com.poozh.translator.data

data class ModelProviderPreset(
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val requiresApiKey: Boolean = true
)

object ModelProviders {
    val presets = listOf(
        ModelProviderPreset(
            name = "DeepSeek",
            baseUrl = AppSettings.DEFAULT_BASE_URL,
            defaultModel = AppSettings.DEFAULT_MODEL
        ),
        ModelProviderPreset(
            name = "智谱 BigModel",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            defaultModel = "glm-4-flash"
        ),
        ModelProviderPreset(
            name = "Moonshot Kimi",
            baseUrl = "https://api.moonshot.cn/v1/chat/completions",
            defaultModel = "moonshot-v1-8k"
        ),
        ModelProviderPreset(
            name = "OpenAI 兼容",
            baseUrl = "https://api.openai.com/v1/chat/completions",
            defaultModel = "gpt-4.1-mini"
        ),
        ModelProviderPreset(
            name = "Ollama 本地",
            baseUrl = "http://127.0.0.1:11434/v1/chat/completions",
            defaultModel = "qwen2.5:7b",
            requiresApiKey = false
        )
    )

    fun match(baseUrl: String): ModelProviderPreset? {
        val normalized = normalize(baseUrl)
        return presets.firstOrNull { normalize(it.baseUrl) == normalized }
    }

    fun displayName(baseUrl: String): String {
        return match(baseUrl)?.name ?: "自定义模型服务"
    }

    fun requiresApiKey(baseUrl: String): Boolean {
        return match(baseUrl)?.requiresApiKey ?: false
    }

    private fun normalize(value: String): String {
        return value.trim().trimEnd('/').lowercase()
    }
}
