package com.cucumberbddparallel.examples.aihealing;

import com.cucumberbddparallel.examples.aihealing.support.DemoFixtureServer;
import com.cucumberbddparallel.framework.ai.AiConfig;
import com.cucumberbddparallel.framework.driver.DriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional live run against your configured provider (Anthropic, OpenAI BYOK, or Ollama).
 * Enable with: {@code AI_HEALING_DEMO_LIVE=true} plus provider credentials in the environment.
 */
@EnabledIfEnvironmentVariable(named = "AI_HEALING_DEMO_LIVE", matches = "true")
class LiveAiHealingDemoTest {

    private DemoFixtureServer fixtureServer;
    private WebDriver driver;

    @BeforeEach
    void setUp() throws Exception {
        assertTrue(AiConfig.isHealingEnabled(),
                "Set AI_HEALING_PROVIDER and credentials before AI_HEALING_DEMO_LIVE=true");
        fixtureServer = DemoFixtureServer.start(AiHealingSupport.fixtureHtml());
        driver = AiHealingSupport.startHeadlessChrome();
        driver.get(fixtureServer.baseUrl());
    }

    @AfterEach
    void tearDown() {
        DriverManager.quit();
        if (fixtureServer != null) {
            fixtureServer.stop();
        }
    }

    @Test
    void brokenLogoLocatorHealsViaLiveLlm() {
        DemoPage page = new DemoPage();
        page.assertLogoVisible();
    }
}