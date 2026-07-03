package com.cucumberbddparallel.framework.ai;

/**
 * Which LLM backend powers locator healing. Pick explicitly with {@code AI_HEALING_PROVIDER},
 * or let {@link AiConfig} infer from your environment (BYOK keys, local Ollama, etc.).
 */
public enum AiProvider {
    ANTHROPIC,
    OPENAI,
    OLLAMA
}