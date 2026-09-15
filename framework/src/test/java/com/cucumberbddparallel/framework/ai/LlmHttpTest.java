package com.cucumberbddparallel.framework.ai;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmHttpTest {

    @Test
    void defaultsToThirtySecondsWhenUnset() {
        assertEquals(Duration.ofSeconds(30), LlmHttp.requestTimeout(key -> null));
    }

    @Test
    void honorsConfiguredTimeout() {
        assertEquals(Duration.ofSeconds(5), LlmHttp.requestTimeout(key -> "5"));
    }

    @Test
    void fallsBackToDefaultOnGarbageValue() {
        assertEquals(Duration.ofSeconds(30), LlmHttp.requestTimeout(key -> "soon"));
    }

    @Test
    void fallsBackToDefaultOnNonPositiveValue() {
        assertEquals(Duration.ofSeconds(30), LlmHttp.requestTimeout(key -> "0"));
        assertEquals(Duration.ofSeconds(30), LlmHttp.requestTimeout(key -> "-10"));
    }

    @Test
    void trimsWhitespaceAroundValue() {
        assertEquals(Duration.ofSeconds(45), LlmHttp.requestTimeout(key -> "  45  "));
    }
}
