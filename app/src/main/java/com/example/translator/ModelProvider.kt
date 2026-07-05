package com.example.translator

/**
 * A model provider: an OpenAI-compatible `/chat/completions` endpoint plus the
 * metadata needed to use it (default model, JSON-mode support, whether an API
 * key is required). All providers below speak the same wire protocol, so
 * [DeepSeekClient] needs no per-vendor code — it just points Retrofit at the
 * provider's [baseUrl] and toggles `response_format` based on
 * [supportsJsonMode].
 *
 * Note on Claude: Anthropic's native API is *not* OpenAI-compatible, so it is
 * not a preset. Use the "Custom" provider pointed at a Claude OpenAI-compatible
 * gateway/proxy if needed.
 */
data class ModelProvider(
    /** Stable id used as the storage key for this provider's key/model. */
    val id: String,
    val displayName: String,
    /** Base URL ending in `/`; the client appends `chat/completions`. */
    val baseUrl: String,
    val defaultModel: String,
    /** Suggested models shown as quick picks in the UI. */
    val models: List<String>,
    /** Whether the endpoint accepts `response_format: json_object`. When false,
     *  the prompt still asks for JSON and [DeepSeekClient.extractJsonObject]
     *  parses it tolerantly. */
    val supportsJsonMode: Boolean = true,
    /** Local servers (Ollama) don't need a key. */
    val needsApiKey: Boolean = true
)

object ModelProviders {

    const val CUSTOM_ID = "custom"

    private val DEEPSEEK = ModelProvider(
        id = "deepseek",
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com/",
        defaultModel = "deepseek-chat",
        models = listOf("deepseek-chat", "deepseek-reasoner"),
        supportsJsonMode = true
    )

    private val OPENAI = ModelProvider(
        id = "openai",
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/v1/",
        defaultModel = "gpt-4o-mini",
        models = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1"),
        supportsJsonMode = true
    )

    private val OPENROUTER = ModelProvider(
        id = "openrouter",
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1/",
        defaultModel = "openai/gpt-4o-mini",
        models = listOf("openai/gpt-4o-mini", "anthropic/claude-3.5-sonnet", "google/gemini-flash-1.5"),
        supportsJsonMode = true
    )

    private val QWEN = ModelProvider(
        id = "qwen",
        displayName = "通义千问 (DashScope)",
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/",
        defaultModel = "qwen-plus",
        models = listOf("qwen-plus", "qwen-turbo", "qwen-max"),
        supportsJsonMode = true
    )

    private val ZHIPU = ModelProvider(
        id = "zhipu",
        displayName = "智谱 (BigModel)",
        baseUrl = "https://open.bigmodel.cn/api/paas/v4/",
        defaultModel = "glm-4-flash",
        models = listOf("glm-4-flash", "glm-4-plus", "glm-4"),
        supportsJsonMode = true
    )

    private val MOONSHOT = ModelProvider(
        id = "moonshot",
        displayName = "Moonshot (Kimi)",
        baseUrl = "https://api.moonshot.cn/v1/",
        defaultModel = "moonshot-v1-8k",
        models = listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"),
        supportsJsonMode = true
    )

    private val OLLAMA = ModelProvider(
        id = "ollama",
        displayName = "Ollama (本地)",
        baseUrl = "http://localhost:11434/v1/",
        defaultModel = "qwen2.5",
        models = listOf("qwen2.5", "llama3.1", "gemma2"),
        // Local models vary; json_object support is unreliable — skip it and
        // rely on the prompt + tolerant parsing instead.
        supportsJsonMode = false,
        needsApiKey = false
    )

    /** The "Custom" preset is a placeholder filled in by the user. */
    val CUSTOM = ModelProvider(
        id = CUSTOM_ID,
        displayName = "自定义",
        baseUrl = "",
        defaultModel = "",
        models = emptyList(),
        supportsJsonMode = true
    )

    /** All built-in presets in display order. */
    val all: List<ModelProvider> = listOf(
        DEEPSEEK, OPENAI, OPENROUTER, QWEN, ZHIPU, MOONSHOT, OLLAMA, CUSTOM
    )

    /** Look up a preset by id; falls back to DeepSeek (the default). */
    fun byId(id: String?): ModelProvider =
        all.firstOrNull { it.id == id } ?: DEEPSEEK

    /** The id to store when nothing has been chosen yet. */
    const val DEFAULT_ID = "deepseek"
}
