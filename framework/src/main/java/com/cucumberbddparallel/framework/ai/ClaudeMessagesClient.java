package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.TokenUsage;

interface ClaudeMessagesClient {

    ClaudeResponse send(String system, String userMessage);

    record ClaudeResponse(String text, TokenUsage usage) {
    }
}
