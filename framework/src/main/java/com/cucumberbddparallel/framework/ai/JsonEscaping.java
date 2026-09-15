package com.cucumberbddparallel.framework.ai;

/**
 * A tiny hand-rolled JSON string escaper/unescaper - just enough to safely put arbitrary
 * text (page HTML, error messages, whatever) inside a JSON string literal, and get it back
 * out again. We're not parsing or building general JSON here, just one string at a time,
 * which is why this doesn't pull in a JSON library.
 */
final class JsonEscaping {

    private JsonEscaping() {
    }

    /** Escapes a raw string so it's safe to drop straight into a {@code "..."} JSON literal. */
    static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else if (Character.isHighSurrogate(c)
                            && i + 1 < value.length()
                            && Character.isLowSurrogate(value.charAt(i + 1))) {
                        // A well-formed surrogate pair is real text, not an escape - keep it.
                        out.append(c).append(value.charAt(++i));
                    } else if (Character.isSurrogate(c)) {
                        // A lone surrogate isn't valid in JSON text at all; emit it as a
                        // backslash-uXXXX escape so the output is always well-formed JSON.
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    /**
     * The inverse of {@link #escape}: turns an escaped JSON string body back into plain text.
     * Strict about malformed input - a trailing backslash, an unknown escape, or a broken
     * backslash-uXXXX sequence throws {@link IllegalArgumentException} instead of silently
     * producing garbage.
     */
    static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (i + 1 >= value.length()) {
                throw new IllegalArgumentException("Trailing backslash in JSON string: '" + value + "'");
            }
            char next = value.charAt(++i);
            switch (next) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (i + 4 >= value.length()) {
                        throw new IllegalArgumentException(
                                "Truncated \\u escape in JSON string: '" + value + "'");
                    }
                    String hex = value.substring(i + 1, i + 5);
                    try {
                        out.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException notHex) {
                        throw new IllegalArgumentException(
                                "Invalid \\u escape '\\u" + hex + "' in JSON string: '" + value + "'", notHex);
                    }
                    i += 4;
                }
                default -> throw new IllegalArgumentException(
                        "Unknown escape sequence '\\" + next + "' in JSON string: '" + value + "'");
            }
        }
        return out.toString();
    }
}
