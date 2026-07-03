# Web patterns demo

CI-safe JUnit demos for common UI automation patterns — all served from a local
`WebPatternsFixtureServer` (no google.com, no API keys).

| Test | Pattern | Framework helper |
|------|---------|------------------|
| `TablePatternsTest` | HTML tables / grids | `TableHelper` |
| `DragDropTest` | Drag and drop lanes | `DragDropHelper` |
| `FileUploadDownloadTest` | Upload + download dir | `FileUploadHelper` |
| `PdfValidationTest` | PDF text (PDFBox) | — |
| `QrCodeTest` | QR decode (ZXing) | — |
| `OcrValidationTest` | OCR (Tesseract) | Docker / `-Pocr-demo` only |

## Run locally

```bash
./mvnw -pl examples/web-patterns-demo -am test
```

OCR (requires Tesseract on PATH):

```bash
./mvnw -pl examples/web-patterns-demo -am test -Pocr-demo
```

## Docker

From repo root:

```bash
docker compose build
docker compose run --rm cucumber-examples
```

See `docs/MCP_PLAYBOOK.md` for wiring these fixtures to [mcp-selenium](https://github.com/angiejones/mcp-selenium).