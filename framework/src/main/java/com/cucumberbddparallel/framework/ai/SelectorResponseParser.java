package com.cucumberbddparallel.framework.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls the actual CSS selector out of whatever Claude replied with.
 *
 * The system prompt asks for a selector wrapped in a ``` code fence and nothing else, and
 * models are generally good about following that - but "generally good" isn't "always," so
 * this falls back to just trimming the whole reply if there's no fence. That way a slightly
 * chatty answer (or one missing the fence for whatever reason) still has a shot at working
 * instead of failing outright.
 */
final class SelectorResponseParser {

    private static final Pattern CODE_FENCE = Pattern.compile("```\\s*(.+?)\\s*```", Pattern.DOTALL);
    // Models label the fence however they like - ```css, ```CSS, ```javascript, or nothing
    // at all. The label (if any) is always on the fence's own line, so it's safe to strip a
    // leading word followed by a line break; a selector on the fence's line is left alone.
    private static final Pattern LANGUAGE_TAG = Pattern.compile("(?i)^[a-z][\\w+.-]*\\R");

    private SelectorResponseParser() {
    }

    /** Extracts the selector from a code fence if there is one, otherwise just trims the reply. */
    static String selectorFrom(String claudeReplyText) {
        Matcher fenced = CODE_FENCE.matcher(claudeReplyText);
        String extracted = fenced.find() ? fenced.group(1) : claudeReplyText;
        Matcher tag = LANGUAGE_TAG.matcher(extracted);
        String withoutTag = tag.find() ? extracted.substring(tag.end()) : extracted;
        return withoutTag.trim();
    }
}
