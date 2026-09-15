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

    @Test
    void stripsNonCssFenceLabels() {
        assertEquals("#new-search-input", SelectorResponseParser.selectorFrom("```javascript\n#new-search-input\n```"));
        assertEquals("#new-search-input", SelectorResponseParser.selectorFrom("```CSS\n#new-search-input\n```"));
        assertEquals("#new-search-input", SelectorResponseParser.selectorFrom("```html\n#new-search-input\n```"));
    }

    @Test
    void doesNotMistakeSelectorStartForLanguageTag() {
        // No line break after the fence - the "word" is part of the selector, not a label.
        assertEquals("input[name=q2]", SelectorResponseParser.selectorFrom("```input[name=q2]```"));
    }
}
