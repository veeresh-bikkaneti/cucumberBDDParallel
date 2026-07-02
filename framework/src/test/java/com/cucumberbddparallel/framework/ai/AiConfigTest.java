package com.cucumberbddparallel.framework.ai;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConfigTest {

    @Test
    void disabledWithNoApiKey() {
        boolean enabled = AiConfig.isHealingEnabled(key -> null, key -> null);

        assertFalse(enabled);
    }

    @Test
    void enabledWithApiKeyAndNoOptOut() {
        boolean enabled = AiConfig.isHealingEnabled(
                key -> "ANTHROPIC_API_KEY".equals(key) ? "sk-ant-test" : null,
                key -> null);

        assertTrue(enabled);
    }

    @Test
    void apiKeyPresentButExplicitlyOptedOut() {
        boolean enabled = AiConfig.isHealingEnabled(
                key -> "ANTHROPIC_API_KEY".equals(key) ? "sk-ant-test" : null,
                key -> "ai.healing.enabled".equals(key) ? "false" : null);

        assertFalse(enabled);
    }

    @Test
    void modelDefaultsToSonnetWhenUnset() {
        assertEquals("claude-sonnet-5", AiConfig.model(key -> null));
    }

    @Test
    void modelUsesOverrideWhenSet() {
        Map<String, String> env = Map.of("ANTHROPIC_MODEL", "claude-haiku-4-5");

        assertEquals("claude-haiku-4-5", AiConfig.model(env::get));
    }
}
