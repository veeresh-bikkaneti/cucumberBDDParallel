package com.cucumberbddparallel.framework.ai;

import com.cucumberbddparallel.framework.ai.cost.TokenUsage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnthropicHttpClientTest {

    @Test
    void parsesUsageWhenFieldsAreAdjacent() {
        String body = "{\"usage\":{\"input_tokens\":10,\"output_tokens\":20}}";

        assertEquals(new TokenUsage(10, 20), AnthropicHttpClient.parseUsage(body));
    }

    @Test
    void parsesUsageWhenCacheTokenFieldsSitBetweenInputAndOutput() {
        String body = "{\"usage\":{\"input_tokens\":10,\"cache_creation_input_tokens\":5,"
                + "\"cache_read_input_tokens\":2,\"output_tokens\":20}}";

        assertEquals(new TokenUsage(10, 20), AnthropicHttpClient.parseUsage(body));
    }

    @Test
    void defaultsToZeroWhenUsageIsMissing() {
        String body = "{\"content\":[]}";

        assertEquals(new TokenUsage(0, 0), AnthropicHttpClient.parseUsage(body));
    }
}
