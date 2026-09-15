package com.cucumberbddparallel.framework.ai;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Immutable resolved configuration for one healing session. Built once from environment
 * variables; {@link AiConfig} delegates here instead of scattering provider switches.
 *
 * A record because this is pure data - four resolved values, no behavior worth a class
 * hierarchy. The compact constructor guards the invariants (a settings object with a blank
 * model or base URL is a bug, not a configuration), while {@link #resolve} stays the
 * single static factory that builds one from the environment.
 */
record AiHealingSettings(AiProvider provider, String model, String apiKey, String baseUrl) {

    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-5";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_OLLAMA_MODEL = "llama3.2";
    private static final String DEFAULT_OPENAI_BASE = "https://api.openai.com/v1";
    private static final String DEFAULT_OLLAMA_BASE = "http://127.0.0.1:11434/v1";
    private static final String DEFAULT_ANTHROPIC_BASE = "https://api.anthropic.com/v1";

    AiHealingSettings {
        Objects.requireNonNull(provider, "provider");
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        // apiKey is allowed to be null - Ollama needs no key, and bearerToken() handles
        // the empty case for OpenAI-compatible endpoints.
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
    }

    /** Bearer token for the OpenAI-compatible {@code Authorization} header; empty when not required. */
    Optional<String> bearerToken() {
        if (provider == AiProvider.OLLAMA || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(apiKey);
    }

    static Optional<AiHealingSettings> resolve(
            Function<String, String> env,
            Function<String, String> systemProperty) {
        if ("false".equalsIgnoreCase(systemProperty.apply("ai.healing.enabled"))) {
            return Optional.empty();
        }
        return resolveProvider(env).flatMap(provider -> {
            if (!credentialsPresent(provider, env)) {
                return Optional.empty();
            }
            return Optional.of(build(provider, env));
        });
    }

    private static AiHealingSettings build(AiProvider provider, Function<String, String> env) {
        return new AiHealingSettings(
                provider,
                resolveModel(provider, env),
                resolveApiKey(provider, env),
                resolveBaseUrl(provider, env));
    }

    private static String resolveModel(AiProvider provider, Function<String, String> env) {
        String unified = AiConfig.firstNonBlank(env.apply("AI_HEALING_MODEL"));
        if (unified != null) {
            return unified;
        }
        return switch (provider) {
            case ANTHROPIC -> AiConfig.firstNonBlank(env.apply("ANTHROPIC_MODEL"), DEFAULT_ANTHROPIC_MODEL);
            case OPENAI -> AiConfig.firstNonBlank(env.apply("OPENAI_MODEL"), DEFAULT_OPENAI_MODEL);
            case OLLAMA -> AiConfig.firstNonBlank(env.apply("OLLAMA_MODEL"), DEFAULT_OLLAMA_MODEL);
        };
    }

    private static String resolveApiKey(AiProvider provider, Function<String, String> env) {
        String unified = AiConfig.firstNonBlank(env.apply("AI_HEALING_API_KEY"));
        if (unified != null) {
            return unified;
        }
        return switch (provider) {
            case ANTHROPIC -> AiConfig.firstNonBlank(env.apply("ANTHROPIC_API_KEY"));
            case OPENAI -> AiConfig.firstNonBlank(env.apply("OPENAI_API_KEY"));
            case OLLAMA -> null;
        };
    }

    private static String resolveBaseUrl(AiProvider provider, Function<String, String> env) {
        String unified = normalizeBaseUrl(AiConfig.firstNonBlank(env.apply("AI_HEALING_BASE_URL")));
        if (unified != null) {
            return unified;
        }
        return switch (provider) {
            case ANTHROPIC -> DEFAULT_ANTHROPIC_BASE;
            case OPENAI -> DEFAULT_OPENAI_BASE;
            case OLLAMA -> normalizeOllamaBase(AiConfig.firstNonBlank(env.apply("OLLAMA_HOST"), DEFAULT_OLLAMA_BASE));
        };
    }

    private static boolean credentialsPresent(AiProvider provider, Function<String, String> env) {
        return switch (provider) {
            case OLLAMA -> true;
            case ANTHROPIC, OPENAI -> AiConfig.firstNonBlank(
                    env.apply("AI_HEALING_API_KEY"),
                    provider == AiProvider.ANTHROPIC ? env.apply("ANTHROPIC_API_KEY") : env.apply("OPENAI_API_KEY")
            ) != null;
        };
    }

    private static Optional<AiProvider> resolveProvider(Function<String, String> env) {
        String explicit = AiConfig.firstNonBlank(env.apply("AI_HEALING_PROVIDER"));
        if (explicit != null) {
            return Optional.of(parseProvider(explicit));
        }
        if (AiConfig.firstNonBlank(env.apply("ANTHROPIC_API_KEY")) != null) {
            return Optional.of(AiProvider.ANTHROPIC);
        }
        if (AiConfig.firstNonBlank(env.apply("OPENAI_API_KEY"), env.apply("AI_HEALING_API_KEY")) != null) {
            return Optional.of(AiProvider.OPENAI);
        }
        if (AiConfig.firstNonBlank(env.apply("OLLAMA_HOST")) != null
                || "true".equalsIgnoreCase(env.apply("AI_HEALING_OLLAMA"))) {
            return Optional.of(AiProvider.OLLAMA);
        }
        return Optional.empty();
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
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
