package com.cucumberbddparallel.framework.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SelectorResponseParser {

    private static final Pattern CODE_FENCE = Pattern.compile("```(?:css)?\\s*(.+?)\\s*```", Pattern.DOTALL);

    private SelectorResponseParser() {
    }

    static String selectorFrom(String claudeReplyText) {
        Matcher fenced = CODE_FENCE.matcher(claudeReplyText);
        return (fenced.find() ? fenced.group(1) : claudeReplyText).trim();
    }
}
