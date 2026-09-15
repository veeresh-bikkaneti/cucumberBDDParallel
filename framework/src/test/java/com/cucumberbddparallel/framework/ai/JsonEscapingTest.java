package com.cucumberbddparallel.framework.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void unescapeHandlesAllShortEscapes() {
        assertEquals("\b\f\n\r\t/\"\\", JsonEscaping.unescape("\\b\\f\\n\\r\\t\\/\\\"\\\\"));
    }

    @Test
    void unescapeParsesUnicodeEscapes() {
        assertEquals("A©", JsonEscaping.unescape("A\\u00a9"));
    }

    @Test
    void unescapeRoundTripsLoneSurrogates() {
        String loneHigh = "\uD800";

        assertEquals(loneHigh, JsonEscaping.unescape(JsonEscaping.escape(loneHigh)));
    }

    @Test
    void escapeKeepsWellFormedSurrogatePairsIntact() {
        String emoji = "😀";

        assertEquals(emoji, JsonEscaping.unescape(JsonEscaping.escape(emoji)));
    }

    @Test
    void unescapeRejectsTrailingBackslash() {
        assertThrows(IllegalArgumentException.class, () -> JsonEscaping.unescape("abc\\"));
    }

    @Test
    void unescapeRejectsUnknownEscape() {
        assertThrows(IllegalArgumentException.class, () -> JsonEscaping.unescape("abc\\q"));
    }

    @Test
    void unescapeRejectsTruncatedUnicodeEscape() {
        assertThrows(IllegalArgumentException.class, () -> JsonEscaping.unescape("abc\\u12"));
    }

    @Test
    void unescapeRejectsNonHexUnicodeEscape() {
        assertThrows(IllegalArgumentException.class, () -> JsonEscaping.unescape("abc\\uzzzz"));
    }
}
