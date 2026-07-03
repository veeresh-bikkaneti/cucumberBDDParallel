package com.cucumberbddparallel.framework.ai;

import java.util.Optional;
import java.util.function.Function;

/**
 * Entry point for AI locator-healing configuration. Resolves settings from environment
 * variables and JVM system properties via {@link AiHealingSettings}.
 *
 * <p>Primary variables: {@code AI_HEALING_PROVIDER}, {@code AI_HEALING_MODEL},
 * {@code AI_HEALING_API_KEY}, {@code AI_HEALING_BASE_URL}.
 *
 * <p>Legacy: {@code ANTHROPIC_API_KEY}, {@code ANTHROPIC_MODEL}, {@code OPENAI_API_KEY},
 * {@code OLLAMA_HOST}. See {@code docs/AI_HEALING.md}.
 */
public final class AiConfig {

    private AiConfig() {
    }

    public static boolean isHealingEnabled() {
        return isHealingEnabled(settings(), System::getProperty);
    }

    static boolean isHealingEnabled(Function<String, String> env, Function<String, String> systemProperty) {
        return AiHealingSettings.resolve(env, systemProperty).isPresent();
    }

    public static AiProvider provider() {
        return requireSettings().provider();
    }

    static AiProvider provider(Function<String, String> env) {
        return AiHealingSettings.resolve(env, key -> null)
                .orElseThrow(() -> new IllegalStateException(
                        "AI healing is not configured — set AI_HEALING_PROVIDER and credentials"))
                .provider();
    }

    public static String model() {
        return requireSettings().model();
    }

    static String model(Function<String, String> env) {
        return AiHealingSettings.resolve(env, key -> null)
                .map(AiHealingSettings::model)
                .orElseThrow(() -> new IllegalStateException(
                        "AI healing is not configured — cannot resolve model"));
    }

    public static String apiKey() {
        return requireSettings().apiKey();
    }

    static String apiKey(Function<String, String> env) {
        return AiHealingSettings.resolve(env, key -> null)
                .map(AiHealingSettings::apiKey)
                .orElseThrow(() -> new IllegalStateException(
                        "AI healing is not configured — cannot resolve API key"));
    }

    public static String baseUrl() {
        return requireSettings().baseUrl();
    }

    static String baseUrl(Function<String, String> env) {
        return AiHealingSettings.resolve(env, key -> null)
                .map(AiHealingSettings::baseUrl)
                .orElseThrow(() -> new IllegalStateException(
                        "AI healing is not configured — cannot resolve base URL"));
    }

    static Optional<String> bearerToken() {
        return AiHealingSettings.resolve(settings(), System::getProperty).flatMap(AiHealingSettings::bearerToken);
    }

    /** Environment variable first, then JVM system property (same key name). */
    static Function<String, String> settings() {
        return key -> firstNonBlank(System.getenv(key), System.getProperty(key));
    }

    private static AiHealingSettings requireSettings() {
        return AiHealingSettings.resolve(settings(), System::getProperty)
                .orElseThrow(() -> new IllegalStateException(
                        "AI healing is not enabled — set provider credentials or AI_HEALING_PROVIDER"));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}