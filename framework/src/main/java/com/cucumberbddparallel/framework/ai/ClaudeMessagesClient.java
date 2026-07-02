package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.TokenUsage;

/**
 * The one thing {@link AiLocatorHealer} actually needs from "some way to talk to Claude":
 * send a system prompt and a user message, get text back plus how many tokens it cost.
 *
 * It's an interface (with exactly one real implementation, {@link AnthropicHttpClient})
 * mostly so tests further up the chain don't have to make real HTTP calls to Anthropic
 * just to check that, say, {@code AiLocatorHealer} logs cost correctly. A fake/mock
 * implementation of this interface is all you need for that.
 */
interface ClaudeMessagesClient {

    ClaudeResponse send(String system, String userMessage);

    /** @param text the plain-text reply; {@code usage} is the input/output token counts for cost tracking. */
    record ClaudeResponse(String text, TokenUsage usage) {
    }
}
