package com.cucumberparallel.ai;

/**
 * AI self-healing is opt-in and reads its credentials from the environment only —
 * never hardcode an API key or model name here.
 */
public final class AiConfig {

    private static final String DEFAULT_MODEL = "claude-opus-4-8";

    private AiConfig() {
    }

    public static boolean isHealingEnabled() {
        boolean hasApiKey = System.getenv("ANTHROPIC_API_KEY") != null;
        boolean optedOut = "false".equalsIgnoreCase(System.getProperty("ai.healing.enabled"));
        return hasApiKey && !optedOut;
    }

    public static String model() {
        String override = System.getenv("ANTHROPIC_MODEL");
        return override == null || override.isBlank() ? DEFAULT_MODEL : override;
    }
}
