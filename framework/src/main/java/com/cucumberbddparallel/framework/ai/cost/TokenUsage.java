package com.cucumberbddparallel.framework.ai.cost;

/** How many tokens one Claude call used - straight from the API's own {@code usage} block. */
public record TokenUsage(int inputTokens, int outputTokens) {
}
