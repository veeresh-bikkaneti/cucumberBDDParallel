package com.cucumberbddparallel.framework.ai;

import java.util.Locale;
import java.util.function.Function;

/**
 * Turns AI self-healing on or off and resolves provider, model, API key, and base URL
 * from the environment. Nothing is hardcoded in source — users bring their own route:
 * Anthropic BYOK, OpenAI-compatible BYOK, or local Ollama.
 *
 * <p>Primary variables:
 * <ul>
 *   <li>{@code AI_HEALING_PROVIDER} — {@code anthropic}, {@code openai}, or {@code ollama}</li>
 *   <li>{@code AI_HEALING_MODEL} — model name for the chosen provider</li>
 *   <li>{@code AI_HEALING_API_KEY} — BYOK key (provider-specific keys still supported)</li>
 *   <li>{@code AI_HEALING_BASE_URL} — OpenAI-compatible base URL (Ollama, Azure OpenAI, etc.)</li>
 * </ul>
 *
 * <p>Legacy: {@code ANTHROPIC_API_KEY}, {@code ANTHROPIC_MODEL}, {@code OPENAI_API_KEY},
 * {@code OLLAMA_HOST}.
 */
public final class AiConfig {

    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-5";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_OLLAMA_MODEL = "llama3.2";
    private static final String DEFAULT_OPENAI_BASE = "https://api.openai.com/v1";
    private static final String DEFAULT_OLLAMA_BASE = "http://127.0.0.1:11434/v1";

    private AiConfig() {
    }

    public static boolean isHealingEnabled() {
        return isHealingEnabled(System::getenv, System::getProperty);
    }

    static boolean isHealingEnabled(Function<String, String> env, Function<String, String> systemProperty) {
        if ("false".equalsIgnoreCase(systemProperty.apply("ai.healing.enabled"))) {
            return false;
        }
        return resolveProvider(env).map(provider -> credentialsPresent(provider, env)).orElse(false);
    }

    public static AiProvider provider() {
        return provider(System::getenv);
    }

    static AiProvider provider(Function<String, String> env) {
        return resolveProvider(env).orElse(AiProvider.ANTHROPIC);
    }

    public static String model() {
        return model(System::getenv);
    }

    static String model(Function<String, String> env) {
        AiProvider provider = provider(env);
        String unified = firstNonBlank(env.apply("AI_HEALING_MODEL"));
        if (unified != null) {
            return unified;
        }
        return switch (provider) {
            case ANTHROPIC -> firstNonBlank(env.apply("ANTHROPIC_MODEL"), DEFAULT_ANTHROPIC_MODEL);
            case OPENAI -> firstNonBlank(env.apply("OPENAI_MODEL"), DEFAULT_OPENAI_MODEL);
            case OLLAMA -> firstNonBlank(env.apply("OLLAMA_MODEL"), DEFAULT_OLLAMA_MODEL);
        };
    }

    public static String apiKey() {
        return apiKey(System::getenv);
    }

    static String apiKey(Function<String, String> env) {
        AiProvider provider = provider(env);
        String unified = firstNonBlank(env.apply("AI_HEALING_API_KEY"));
        if (unified != null) {
            return unified;
        }
        return switch (provider) {
            case ANTHROPIC -> firstNonBlank(env.apply("ANTHROPIC_API_KEY"));
            case OPENAI -> firstNonBlank(env.apply("OPENAI_API_KEY"));
            case OLLAMA -> "ollama";
        };
    }

    public static String baseUrl() {
        return baseUrl(System::getenv);
    }

    static String baseUrl(Function<String, String> env) {
        AiProvider provider = provider(env);
        String unified = normalizeBaseUrl(firstNonBlank(env.apply("AI_HEALING_BASE_URL")));
        if (unified != null) {
            return unified;
        }
        return switch (provider) {
            case ANTHROPIC -> "https://api.anthropic.com/v1";
            case OPENAI -> DEFAULT_OPENAI_BASE;
            case OLLAMA -> normalizeOllamaBase(firstNonBlank(env.apply("OLLAMA_HOST"), DEFAULT_OLLAMA_BASE));
        };
    }

    private static boolean credentialsPresent(AiProvider provider, Function<String, String> env) {
        return switch (provider) {
            case OLLAMA -> true;
            case ANTHROPIC, OPENAI -> firstNonBlank(
                    env.apply("AI_HEALING_API_KEY"),
                    provider == AiProvider.ANTHROPIC ? env.apply("ANTHROPIC_API_KEY") : env.apply("OPENAI_API_KEY")
            ) != null;
        };
    }

    private static java.util.Optional<AiProvider> resolveProvider(Function<String, String> env) {
        String explicit = firstNonBlank(env.apply("AI_HEALING_PROVIDER"));
        if (explicit != null) {
            return java.util.Optional.of(parseProvider(explicit));
        }
        if (firstNonBlank(env.apply("ANTHROPIC_API_KEY")) != null) {
            return java.util.Optional.of(AiProvider.ANTHROPIC);
        }
        if (firstNonBlank(env.apply("OPENAI_API_KEY"), env.apply("AI_HEALING_API_KEY")) != null) {
            return java.util.Optional.of(AiProvider.OPENAI);
        }
        if (firstNonBlank(env.apply("OLLAMA_HOST")) != null
                || "true".equalsIgnoreCase(env.apply("AI_HEALING_OLLAMA"))) {
            return java.util.Optional.of(AiProvider.OLLAMA);
        }
        return java.util.Optional.empty();
    }

    private static AiProvider parseProvider(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "anthropic", "claude" -> AiProvider.ANTHROPIC;
            case "openai", "openai-compatible", "compatible" -> AiProvider.OPENAI;
            case "ollama", "local" -> AiProvider.OLLAMA;
            default -> throw new IllegalStateException(
                    "Unknown AI_HEALING_PROVIDER: " + value + ". Use anthropic, openai, or ollama.");
        };
    }

    private static String normalizeOllamaBase(String hostOrBase) {
        String normalized = normalizeBaseUrl(hostOrBase);
        if (normalized == null) {
            return DEFAULT_OLLAMA_BASE;
        }
        if (normalized.endsWith("/v1")) {
            return normalized;
        }
        return normalized.endsWith("/") ? normalized + "v1" : normalized + "/v1";
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @SafeVarargs
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}