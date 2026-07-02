package com.cucumberbddparallel.framework.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectorResponseParserTest {

    @Test
    void extractsSelectorFromCodeFence() {
        String reply = "```css\n#new-search-input\n```";

        assertEquals("#new-search-input", SelectorResponseParser.selectorFrom(reply));
    }

    @Test
    void extractsSelectorFromPlainCodeFenceWithoutLanguage() {
        String reply = "```\ninput[name=q2]\n```";

        assertEquals("input[name=q2]", SelectorResponseParser.selectorFrom(reply));
    }

    @Test
    void fallsBackToTrimmedTextWhenNoFence() {
        String reply = "  input[name=q2]  ";

        assertEquals("input[name=q2]", SelectorResponseParser.selectorFrom(reply));
    }
}
