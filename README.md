# cucumberBDDParallel
Cucumber + Selenium 4 + TestNG parallel BDD framework, with optional AI-powered
self-healing element locators (Claude API, called directly via java.net.http —
no vendor SDK dependency).

## Requirements

- JDK 21
- Maven 3.9+

## Running the tests

```
mvn clean verify -Pintegration-test -DskipTests -U
```

Browser selection was already configurable via `-Dbrowser=firefox` (defaults to `chrome`).

## AI self-healing locators

When a page's `@FindBy` locator can no longer find its element (e.g. after a
markup change), the framework can ask Claude for a replacement CSS selector
and retry once before failing the step.

This is **off by default** and only activates when an API key is present in
the environment — never commit a key to source control:

```
export ANTHROPIC_API_KEY=sk-ant-...
# optional overrides, both read from the environment (no hardcoded defaults to edit):
export ANTHROPIC_MODEL=claude-opus-4-8   # defaults to claude-opus-4-8 if unset
mvn clean verify -Pintegration-test -DskipTests -U
```

To explicitly disable healing even when a key is set: `-Dai.healing.enabled=false`.

AI is one of two interchangeable implementations of Selenium's standard
`ElementLocatorFactory` interface — `BasePage` picks between Selenium's own
`DefaultElementLocatorFactory` and `com.cucumberparallel.ai.AiElementLocatorFactory`
at runtime based on whether an API key is configured. Page objects and step
definitions are identical either way; nothing forces AI usage, it's an
additive capability.

## Notes on dependency versions

Versions were checked against live Maven Central metadata and the project
was built end-to-end (`mvn verify -Pintegration-test`, dry-run) to confirm
everything resolves and compiles.
