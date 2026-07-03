package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.TokenUsage;

/**
 * Provider-agnostic chat API used by {@link AiLocatorHealer}. Implementations talk to
 * Anthropic, OpenAI-compatible cloud APIs (BYOK), or a local Ollama instance.
 */
interface LlmMessagesClient {

    LlmResponse send(String system, String userMessage);

    record LlmResponse(String text, TokenUsage usage) {
    }
}