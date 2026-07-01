# Modernize framework + example split

## Context

Right now this is one Maven module. Thirteen classes, all under
`src/test/java`, mixing framework concerns (driver setup, waits, AI
locator healing) with example-specific ones (Google homepage/search
page objects, step defs, feature files). A few real problems came out
of reading the code:

- `Setup.driver` is a plain static field. Fine for the two feature
  files we run today (failsafe forks at the process level), but it's
  a landmine for anyone who turns on real in-JVM parallel execution -
  which is the entire point of a repo called "parallel".
- Zero unit tests. The only coverage is the two Cucumber scenarios,
  which hit live google.com.
- No CI.
- `AiConfig` defaults the healer to Opus, the priciest tier, for a
  job that's just "read some HTML, return a CSS selector."
- `AiLocatorHealer` does HTTP + JSON escaping + response parsing in
  one class, so none of it can be tested without a real network call.

## Goal

Split into a framework people can actually depend on, plus an example
project that shows how to consume it - with and without AI locator
healing turned on, and with the cost of that AI usage visible instead
of hidden.

## Non-goals

- Not swapping the demo off google.com. It's a fine "AUT" for a demo
  project - anyone using this as a template points it at their own
  site.
- Not building any dashboard/UI for cost data. Log lines + a table in
  the docs is enough.
- Not touching Cucable/parallel test-splitting mechanics beyond
  moving its config to the right module.

## Module layout

```
pom.xml                        parent/aggregator, packaging=pom
framework/
  pom.xml
  src/main/java/com/cucumberbddparallel/framework/
    driver/DriverManager.java  ThreadLocal<WebDriver>, replaces static field
    driver/Setup.java          @Before hook, delegates to DriverManager
    driver/TearDown.java       @After hook, delegates to DriverManager
    wait/Wait.java             unchanged logic, moved
    page/BasePage.java         unchanged logic, moved, reads driver via DriverManager
    ai/AiConfig.java
    ai/AiElementLocatorFactory.java
    ai/AiLocatorHealer.java          orchestrator only, thinned down
    ai/ClaudeMessagesClient.java     new: HTTP call behind an interface
    ai/SelectorResponseParser.java   new: pulled out of AiLocatorHealer
    ai/JsonEscaping.java             new: pulled out of AiLocatorHealer
    ai/cost/TokenUsage.java          new
    ai/cost/ModelPricing.java        new
    ai/cost/CostCalculator.java      new
    ai/cost/CostLogger.java          new
  src/test/java/...           unit tests, junit5 + mockito, test scope only
example-tests/
  pom.xml                     depends on framework
  src/test/java/com/cucumberbddparallel/example/
    homepage/HomePage.java, HomePageSteps.java
    searchresultpage/SearchResultPage.java, SearchResultPageSteps.java
    runner/HomePageTest.java, SearchTest.java
  src/test/resources/features/*.feature
  src/test/resources/cucable.template
```

`framework` classes move to `src/main/java` (compile scope) so it's a
real jar a consumer can depend on - not test-scoped code masquerading
as a library. `example-tests` is where all the Cucumber
glue/steps/features/runners live, same as any team using this as a
starting point would organize their own tests.

Also fixing the `cucumberbddprallel` groupId typo to
`cucumberbddparallel` while we're moving everything around anyway -
no downstream consumers exist yet (SNAPSHOT), so no compatibility
cost.

`cucable-plugin` and `maven-failsafe-plugin` config moves into
`example-tests/pom.xml` since it operates on that module's feature
files. Root `pom.xml` keeps shared dependency/plugin version
management so both modules stay in sync.

## What changes in the code, and why

**DriverManager.** Replaces `Setup.driver` (static field) with a
`ThreadLocal<WebDriver>`. `Setup` still creates the browser instance
via the `@Before` hook, it just hands it to `DriverManager` instead
of assigning a static. `TearDown` and `BasePage` read through
`DriverManager.get()`. This is the actual thread-safety fix -
everything else is secondary to this one.

**AiLocatorHealer split up.** Today it's one class doing the HTTP
call, building the request JSON by hand, escaping/unescaping strings,
and regex-parsing the response. Splitting into:

- `ClaudeMessagesClient` - interface + a `java.net.http` impl. Takes
  system/user text, returns parsed text + token usage. This is the
  seam that makes the rest testable without hitting the network.
- `SelectorResponseParser` - pulls the CSS selector out of Claude's
  reply (code-fence or plain text). Pure function.
- `JsonEscaping` - the escape/unescape helpers, unchanged logic,
  own file.
- `AiLocatorHealer` - now just wires the above together and retries
  the lookup. Same public behavior as before.

`BasePage`'s locator selection (`AiConfig.isHealingEnabled() ? AI :
Default`) stays exactly as it is - it's already coding against
Selenium's own `ElementLocatorFactory` interface, and there's no
third strategy on the horizon to justify more indirection there.

**Cost tracking.** `ClaudeMessagesClient` reads the `usage` block
Anthropic returns with every response (input/output token counts).
`ModelPricing` is a lookup table of $/million-tokens by model name -
comment on the table says to check current pricing before trusting
the numbers for real budgeting, since these change over time and I'm
not going to pretend a hardcoded table stays accurate forever.
`CostCalculator` turns usage + model into a dollar figure.
`CostLogger` logs one line per healing call (model, tokens in/out,
cost, which element) and keeps a running total for the JVM, flushed
via a shutdown hook so you get a session total whether you're running
locally or in CI. All through SLF4J, which is already a dependency
that nothing currently uses (everything logs via `System.out` right
now).

**Default model.** `AiConfig` switches its default from
`claude-opus-4-8` to `claude-sonnet-5`. `ANTHROPIC_MODEL` still
overrides. Healing a locator is "find one CSS selector in this HTML"
- that doesn't need the most expensive model, and Sonnet costs a
fraction of Opus for a task this size.

## Error handling

Unchanged from today for the healing path itself: if the AI-suggested
selector also fails, the original `NoSuchElementException` is
rethrown with the healing failure attached as a suppressed exception.
New: if `CostCalculator` hits an unknown model (someone sets
`ANTHROPIC_MODEL` to something not in the pricing table), it logs a
warning and skips the cost line rather than failing the test - cost
visibility shouldn't be able to break a test run.

## Testing

`framework` gets `junit-jupiter` + `mockito-core`, test scope:

- `SelectorResponseParserTest` - fenced/unfenced/escaped-quote cases
- `JsonEscapingTest` - round trip
- `CostCalculatorTest` - known model/usage -> expected cost, unknown
  model -> handled gracefully
- `DriverManagerTest` - two threads get two different driver
  instances (needs a small injectable driver-supplier seam so the
  test doesn't spin up real Chrome)
- `AiConfigTest` - env var / system property combinations (needs the
  same kind of seam - a lookup function it defaults to
  `System::getenv`, overridable for the test)

`example-tests` keeps the two existing feature files as-is, run
against real google.com. They're the example, not a correctness gate
on the framework - documented as such so nobody's surprised when a
Google UI change breaks them.

## CI

`.github/workflows/ci.yml`, three jobs:

1. `unit-tests` - `mvn -pl framework test`. Fast, no browser, no
   network, runs on every push/PR.
2. `e2e-no-ai` - `mvn verify -Pintegration-test -pl example-tests -am`
   with no `ANTHROPIC_API_KEY` set. Proves the framework works with
   AI healing off.
3. `e2e-with-ai` - same, with `ANTHROPIC_API_KEY` from a repo secret.
   Skips itself (rather than failing) when the secret isn't present,
   so forks without the secret don't get a red X for something
   outside their control. Cost log lines get pulled into
   `$GITHUB_STEP_SUMMARY` so the per-run dollar cost is visible right
   on the Actions run without digging through logs.

## Docs

**README.md** (beginner-facing): what this is, quickstart to run
without AI, how to turn AI healing on, one mermaid flowchart showing
the two paths (with/without healing) side by side, pointer to the
playbook for anything deeper.

**PLAYBOOK.md** (expert-facing): why the module split, how to depend
on `framework` from your own repo, the SOLID reasoning behind each
change above (short table: principle, where, why), the cost model in
detail (pricing table + how to change the default model + how CI
surfaces cost), CI job breakdown, how to extend (new page object, new
wait condition, new locator strategy), and known rough edges
(live-site flakiness, driver version pinning). One mermaid sequence
diagram for the healing call end-to-end, one flowchart for the CI
jobs.

## Rollout

`git mv` the existing files into their new homes to keep history
where git can follow renames. Package rename
(`com.cucumberparallel.hookup.driver` etc. -> `com.cucumberbddparallel.framework.*`)
happens in the same moves. Root `pom.xml` becomes an aggregator
(`<packaging>pom</packaging>`, `<modules>`). `mvnw`/`.mvn` wrapper
stays at the root and works unchanged for reactor builds.

Branch already renamed from `claude/modernize-codebase-ai-tq4wh9` to
`feature/modernize-framework` (local only so far - the old name is
still on `origin` until that's pushed/cleaned up separately).
