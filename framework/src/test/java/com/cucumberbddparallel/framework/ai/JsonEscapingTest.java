package com.cucumberbddparallel.framework.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonEscapingTest {

    @Test
    void escapesQuotesBackslashesAndControlChars() {
        String input = "line one\nline \"two\"\\three";

        String escaped = JsonEscaping.escape(input);

        assertEquals("line one\\nline \\\"two\\\"\\\\three", escaped);
    }

    @Test
    void unescapeReversesEscape() {
        String input = "line one\nline \"two\"\\three";

        assertEquals(input, JsonEscaping.unescape(JsonEscaping.escape(input)));
    }
}
