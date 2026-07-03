package com.cucumberbddparallel.framework.ai;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConfigTest {

    @Test
    void disabledWithNoCredentials() {
        assertFalse(AiConfig.isHealingEnabled(key -> null, key -> null));
    }

    @Test
    void enabledWithAnthropicApiKey() {
        assertTrue(AiConfig.isHealingEnabled(
                key -> "ANTHROPIC_API_KEY".equals(key) ? "sk-ant-test" : null,
                key -> null));
        assertEquals(AiProvider.ANTHROPIC, AiConfig.provider(key -> "ANTHROPIC_API_KEY".equals(key) ? "sk-ant-test" : null));
    }

    @Test
    void enabledWithOpenAiApiKey() {
        assertTrue(AiConfig.isHealingEnabled(
                key -> "OPENAI_API_KEY".equals(key) ? "sk-openai-test" : null,
                key -> null));
        assertEquals(AiProvider.OPENAI, AiConfig.provider(key -> "OPENAI_API_KEY".equals(key) ? "sk-openai-test" : null));
    }

    @Test
    void enabledWithExplicitOllamaProvider() {
        Map<String, String> env = Map.of("AI_HEALING_PROVIDER", "ollama");
        assertTrue(AiConfig.isHealingEnabled(env::get, key -> null));
        assertEquals(AiProvider.OLLAMA, AiConfig.provider(env::get));
        assertEquals("http://127.0.0.1:11434/v1", AiConfig.baseUrl(env::get));
        assertEquals("llama3.2", AiConfig.model(env::get));
    }

    @Test
    void enabledWithUnifiedByokKeyAndProvider() {
        Map<String, String> env = Map.of(
                "AI_HEALING_PROVIDER", "openai",
                "AI_HEALING_API_KEY", "sk-byok-test");
        assertTrue(AiConfig.isHealingEnabled(env::get, key -> null));
        assertEquals(AiProvider.OPENAI, AiConfig.provider(env::get));
        assertEquals("sk-byok-test", AiConfig.apiKey(env::get));
    }

    @Test
    void apiKeyPresentButExplicitlyOptedOut() {
        assertFalse(AiConfig.isHealingEnabled(
                key -> "ANTHROPIC_API_KEY".equals(key) ? "sk-ant-test" : null,
                key -> "ai.healing.enabled".equals(key) ? "false" : null));
    }

    @Test
    void anthropicModelDefaultsToSonnetWhenUnset() {
        assertEquals("claude-sonnet-5", AiConfig.model(key -> null));
    }

    @Test
    void anthropicModelUsesLegacyOverrideWhenSet() {
        Map<String, String> env = Map.of("ANTHROPIC_MODEL", "claude-haiku-4-5", "ANTHROPIC_API_KEY", "sk-ant-test");
        assertEquals("claude-haiku-4-5", AiConfig.model(env::get));
    }

    @Test
    void unifiedModelOverridesProviderDefaults() {
        Map<String, String> env = Map.of(
                "AI_HEALING_PROVIDER", "ollama",
                "AI_HEALING_MODEL", "mistral");
        assertEquals("mistral", AiConfig.model(env::get));
    }

    @Test
    void ollamaHostNormalizesToV1BaseUrl() {
        Map<String, String> env = Map.of("AI_HEALING_PROVIDER", "ollama", "OLLAMA_HOST", "http://localhost:11434");
        assertEquals("http://localhost:11434/v1", AiConfig.baseUrl(env::get));
    }

    @Test
    void customOpenAiCompatibleBaseUrl() {
        Map<String, String> env = Map.of(
                "AI_HEALING_PROVIDER", "openai",
                "AI_HEALING_API_KEY", "sk-test",
                "AI_HEALING_BASE_URL", "https://my-gateway.example/v1");
        assertEquals("https://my-gateway.example/v1", AiConfig.baseUrl(env::get));
    }

    @Test
    void systemPropertyEnablesOpenAiProviderForDemos() {
        try {
            System.setProperty("AI_HEALING_PROVIDER", "openai");
            System.setProperty("AI_HEALING_API_KEY", "demo-key");
            assertTrue(AiConfig.isHealingEnabled());
            assertEquals(AiProvider.OPENAI, AiConfig.provider());
        } finally {
            System.clearProperty("AI_HEALING_PROVIDER");
            System.clearProperty("AI_HEALING_API_KEY");
        }
    }
}