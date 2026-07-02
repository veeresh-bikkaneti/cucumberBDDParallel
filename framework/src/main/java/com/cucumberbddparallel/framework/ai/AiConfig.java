package com.cucumberbddparallel.framework.ai;

import java.util.function.Function;

/**
 * Turns AI self-healing on or off, and picks which model to use.
 *
 * The rule is simple: if you don't set an ANTHROPIC_API_KEY, nothing AI-related
 * ever runs. No key means no network calls, no cost, no surprises. That's on
 * purpose - a test framework shouldn't silently start calling an external API
 * just because a class is on the classpath.
 *
 * Nothing here is hardcoded. Both the key and the model name come from the
 * environment, so please don't "helpfully" paste an API key into this file.
 */
public final class AiConfig {

    // Sonnet is the default because healing a locator is a small, well-defined task
    // (read some HTML, return one CSS selector) - it doesn't need Opus-level reasoning,
    // and it's a fraction of the cost. See PLAYBOOK.md for the full reasoning.
    private static final String DEFAULT_MODEL = "claude-sonnet-5";

    private AiConfig() {
    }

    /** True only when an API key is present AND nobody has explicitly opted out. */
    public static boolean isHealingEnabled() {
        return isHealingEnabled(System::getenv, System::getProperty);
    }

    // Package-private overload that takes the env/property lookups as parameters instead
    // of calling System.getenv()/System.getProperty() directly. That's what makes this
    // testable without needing to actually set real environment variables in a JUnit run -
    // see AiConfigTest for what that looks like in practice.
    static boolean isHealingEnabled(Function<String, String> env, Function<String, String> systemProperty) {
        boolean hasApiKey = env.apply("ANTHROPIC_API_KEY") != null;
        boolean optedOut = "false".equalsIgnoreCase(systemProperty.apply("ai.healing.enabled"));
        return hasApiKey && !optedOut;
    }

    /** Which Claude model to call, defaulting to Sonnet unless ANTHROPIC_MODEL says otherwise. */
    public static String model() {
        return model(System::getenv);
    }

    static String model(Function<String, String> env) {
        String override = env.apply("ANTHROPIC_MODEL");
        return override == null || override.isBlank() ? DEFAULT_MODEL : override;
    }
}
