# MCP Selenium Playbook

Use [Angie Jones' mcp-selenium](https://github.com/angiejones/mcp-selenium) server to let an AI agent drive a real browser while you keep cucumberBDDParallel as the source of truth for BDD structure, page objects, and CI.

This playbook is **documentation only** — no MCP server ships inside this repo. You wire the MCP server in your IDE (Cursor, Claude Desktop, etc.) and point it at the same Chrome session patterns the examples use.

## What you get

| Capability | Where it lives in this repo |
|------------|----------------------------|
| Tables / grids | `examples/web-patterns-demo` + `TableHelper` |
| Drag and drop | `examples/web-patterns-demo` + `DragDropHelper` |
| Upload / download | `examples/web-patterns-demo` + `FileUploadHelper` |
| PDF text checks | `examples/web-patterns-demo` (`PdfValidationTest`, PDFBox) |
| QR decode | `examples/web-patterns-demo` (`QrCodeTest`, ZXing) |
| OCR (optional) | `examples/web-patterns-demo` (`OcrValidationTest`, Tesseract via Docker) |
| Parallel BDD baseline | `example-tests/` + `framework/` |

## Install mcp-selenium

Follow the upstream README: [github.com/angiejones/mcp-selenium](https://github.com/angiejones/mcp-selenium).

Typical Cursor / Claude Desktop entry (adjust paths for your machine):

```json
{
  "mcpServers": {
    "selenium": {
      "command": "npx",
      "args": ["-y", "@angiejones/mcp-selenium"]
    }
  }
}
```

A copy-ready template lives at [`docs/mcp-selenium-config.example.json`](mcp-selenium-config.example.json).

## Suggested agent workflow

1. **Start fixtures locally** (no google.com flake):

   ```bash
   ./mvnw -pl examples/web-patterns-demo -am test -Dtest=TablePatternsTest
   ```

   Tests spin up `WebPatternsFixtureServer` on a random port — for manual MCP exploration, run any demo test in the IDE and note the logged base URL, or start the module tests and inspect failure screenshots.

2. **Open a browser session** via MCP (`start_browser` / equivalent tool from mcp-selenium).

3. **Navigate** to fixture routes served by the demo server:
   - `/tables.html` — order grid
   - `/drag-drop.html` — checklist lanes
   - `/upload.html` — file input
   - `/download/report.txt` — attachment
   - `/sample.pdf` — PDF validation target
   - `/qr.html` — QR screenshot decode

4. **Translate discoveries into BDD** — capture selectors and flows as Gherkin + page objects in your project module, using `framework` helpers instead of raw MCP calls in CI.

5. **Keep CI deterministic** — MCP sessions are for exploration; committed tests use JUnit demos or Cucumber runners with local fixtures.

## Prompt snippets for agents

**Explore table sorting**

> Open the orders table at `/tables.html`. Read header texts and row values. Propose a Cucumber scenario that asserts ORD-1002 is Pending.

**Drag-drop**

> Drag "QA sign-off" into the done lane on `/drag-drop.html`. Confirm `#drop-status` updates. Suggest a step definition using `DragDropHelper`.

**Upload evidence**

> Upload a small text file via `#file-input` on `/upload.html`. Confirm the result paragraph contains the filename.

## Docker: run everything without local Chrome

```bash
docker compose build
docker compose run --rm cucumber-examples
```

OCR (needs Tesseract inside the image):

```bash
docker compose --profile ocr run --rm cucumber-examples-ocr
```

Full Google `example-tests` integration (network + live site):

```bash
docker compose --profile integration run --rm cucumber-integration
```

## References

- [mcp-selenium (Angie Jones)](https://github.com/angiejones/mcp-selenium)
- [Selenium WebDriver documentation](https://www.selenium.dev/documentation/webdriver/)
- [Model Context Protocol](https://modelcontextprotocol.io/)