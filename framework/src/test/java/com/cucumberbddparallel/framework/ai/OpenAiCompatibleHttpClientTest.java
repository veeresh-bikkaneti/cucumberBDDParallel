package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.TokenUsage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleHttpClientTest {

    @Test
    void extractsSelectorFromMessageContent() {
        String body = """
                {
                  "choices":[{"message":{"role":"assistant","content":"```css\\n#logo\\n```"}}],
                  "usage":{"prompt_tokens":1,"completion_tokens":2}
                }
                """;

        assertTrue(OpenAiCompatibleHttpClient.messageContent(body).isPresent());
        assertEquals("```css\n#logo\n```", OpenAiCompatibleHttpClient.messageContent(body).orElseThrow());
    }

    @Test
    void parsesMessageContentAndUsage() {
        String body = """
                {
                  "choices":[{"message":{"role":"assistant","content":"```css\\ninput[name=q]\\n```"}}],
                  "usage":{"prompt_tokens":10,"completion_tokens":20,"total_tokens":30}
                }
                """;

        assertEquals(new TokenUsage(10, 20), OpenAiCompatibleHttpClient.parseUsage(body));
    }

    @Test
    void usageDefaultsToZeroWhenMissing() {
        String body = "{\"choices\":[{\"message\":{\"content\":\"#logo\"}}]}";

        assertEquals(new TokenUsage(0, 0), OpenAiCompatibleHttpClient.parseUsage(body));
    }
}