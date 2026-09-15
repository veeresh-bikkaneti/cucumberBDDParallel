package com.cucumberbddparallel.examples.app;

/**
 * Renders the {@code /search?q=...} results page. Kept separate from the HTTP plumbing
 * so the markup contract (heading text, result structure, URL format) lives in one
 * obvious place and can be reasoned about without a running server.
 */
final class SearchPageRenderer {

    private SearchPageRenderer() {
    }

    /**
     * Builds the search results HTML for the given query.
     *
     * <p>Contract: the page title contains the query; {@code #results-heading} reads
     * exactly {@code Results for "X"}; each result is a {@code .result} div holding a
     * {@code .result-title} and a {@code .result-url}, and every result URL contains the
     * lowercased query string.
     */
    static String render(String query) {
        String safe = escapeHtml(query);
        String lower = escapeHtml(query.toLowerCase());
        StringBuilder results = new StringBuilder();
        String[] titles = {
                "The complete guide to " + safe,
                safe + " - official documentation",
                "Top 10 " + safe + " resources"};
        for (int i = 0; i < titles.length; i++) {
            results.append("<div class=\"result\">")
                    .append("<div class=\"result-title\">").append(titles[i]).append("</div>")
                    .append("<div class=\"result-url\">https://example.com/")
                    .append(lower).append("/result-").append(i + 1)
                    .append("</div>")
                    .append("</div>\n");
        }
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <title>Search results for %s - Example App</title>
                  <style>
                    body { font-family: sans-serif; max-width: 640px; margin: 2rem auto; }
                    .result { border: 1px solid #ddd; border-radius: 6px; padding: 1rem; margin: 1rem 0; }
                    .result-title { font-weight: bold; font-size: 1.1rem; }
                    .result-url { color: #1a6b1a; font-size: 0.9rem; }
                  </style>
                </head>
                <body>
                  <h1 id="results-heading">Results for "%s"</h1>
                  <a href="/">Back to home</a>
                  %s
                </body>
                </html>
                """.formatted(safe, safe, results);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
