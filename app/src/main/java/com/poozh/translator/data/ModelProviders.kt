package com.poozh.translator.data

data class ModelProviderPreset(
    /** Stable id used as the storage key for this provider's key/model. */
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    /** Suggested models for this provider, shown as quick picks in the UI.
     *  Model ids are often case-sensitive on the provider side, so they must
     *  match the provider's `/models` listing exactly. */
    val models: List<String> = emptyList(),
    val requiresApiKey: Boolean = true,
    /** Whether the endpoint accepts `response_format: json_object`. When false,
     *  the prompt still asks for JSON and parsing is tolerant. */
    val supportsJsonMode: Boolean = true
)

object ModelProviders {

    /** The provider selected on first launch / when nothing is configured yet.
     *  Paratera is seeded with a bundled key (see AppSettings) so it works
     *  out of the box. */
    const val DEFAULT_PROVIDER_ID = "paratera"

    val presets = listOf(
        ModelProviderPreset(
            id = "deepseek",
            name = "DeepSeek",
            baseUrl = AppSettings.DEFAULT_BASE_URL,
            defaultModel = AppSettings.DEFAULT_MODEL
        ),
        ModelProviderPreset(
            id = "paratera",
            name = "并行科技 Paratera",
            // OpenAI-compatible MaaS platform; the baseUrl is the FULL endpoint
            // (DeepSeekClient uses it verbatim, no /chat/completions appending).
            baseUrl = "https://llmapi.paratera.com/v1/chat/completions",
            // Model ids on Paratera are case-sensitive (verified via /v1/models);
            // the lowercase form is rejected with HTTP 400 "no healthy deployments".
            defaultModel = "DeepSeek-V4-Flash",
            models = listOf("DeepSeek-V4-Flash", "DeepSeek-V3.2", "Qwen3-235B-A22B", "GLM-4.5-Flash")
        ),
        ModelProviderPreset(
            id = "zhipu",
            name = "智谱 BigModel",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            defaultModel = "glm-4-flash"
        ),
        ModelProviderPreset(
            id = "moonshot",
            name = "Moonshot Kimi",
            baseUrl = "https://api.moonshot.cn/v1/chat/completions",
            defaultModel = "moonshot-v1-8k"
        ),
        ModelProviderPreset(
            id = "openai",
            name = "OpenAI 兼容",
            baseUrl = "https://api.openai.com/v1/chat/completions",
            defaultModel = "gpt-4.1-mini"
        ),
        ModelProviderPreset(
            id = "ollama",
            name = "Ollama 本地",
            baseUrl = "http://127.0.0.1:11434/v1/chat/completions",
            defaultModel = "qwen2.5:7b",
            requiresApiKey = false,
            // Local models vary; json_object support is unreliable — skip it.
            supportsJsonMode = false
        )
    )

    fun byId(id: String?): ModelProviderPreset =
        presets.firstOrNull { it.id == id } ?: presets.first { it.id == DEFAULT_PROVIDER_ID }

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
