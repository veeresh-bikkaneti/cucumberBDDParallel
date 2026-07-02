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
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    /** The inverse of {@link #escape}: turns an escaped JSON string body back into plain text. */
    static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                out.append(switch (next) {
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    default -> next;
                });
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
