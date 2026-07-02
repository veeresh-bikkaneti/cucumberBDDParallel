package com.cucumberbddparallel.framework.ai;

import java.util.function.Function;

/**
 * AI self-healing is opt-in and reads its credentials from the environment only -
 * never hardcode an API key or model name here.
 */
public final class AiConfig {

    private static final String DEFAULT_MODEL = "claude-sonnet-5";

    private AiConfig() {
    }

    public static boolean isHealingEnabled() {
        return isHealingEnabled(System::getenv, System::getProperty);
    }

    static boolean isHealingEnabled(Function<String, String> env, Function<String, String> systemProperty) {
        boolean hasApiKey = env.apply("ANTHROPIC_API_KEY") != null;
        boolean optedOut = "false".equalsIgnoreCase(systemProperty.apply("ai.healing.enabled"));
        return hasApiKey && !optedOut;
    }

    public static String model() {
        return model(System::getenv);
    }

    static String model(Function<String, String> env) {
        String override = env.apply("ANTHROPIC_MODEL");
        return override == null || override.isBlank() ? DEFAULT_MODEL : override;
    }
}
