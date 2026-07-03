package com.cucumberbddparallel.framework.ai;

/**
 * Picks the HTTP client for the configured {@link AiProvider}.
 */
final class LlmClientFactory {

    private LlmClientFactory() {
    }

    static LlmMessagesClient create() {
        return switch (AiConfig.provider()) {
            case ANTHROPIC -> new AnthropicHttpClient();
            case OPENAI, OLLAMA -> new OpenAiCompatibleHttpClient();
        };
    }
}