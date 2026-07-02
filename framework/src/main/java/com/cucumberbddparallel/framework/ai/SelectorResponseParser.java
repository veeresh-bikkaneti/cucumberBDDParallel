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

    // (?:css)? makes the "css" language tag after ``` optional, since we ask for a selector
    // but can't fully control whether the model labels the fence.
    private static final Pattern CODE_FENCE = Pattern.compile("```(?:css)?\\s*(.+?)\\s*```", Pattern.DOTALL);

    private SelectorResponseParser() {
    }

    /** Extracts the selector from a code fence if there is one, otherwise just trims the reply. */
    static String selectorFrom(String claudeReplyText) {
        Matcher fenced = CODE_FENCE.matcher(claudeReplyText);
        return (fenced.find() ? fenced.group(1) : claudeReplyText).trim();
    }
}
