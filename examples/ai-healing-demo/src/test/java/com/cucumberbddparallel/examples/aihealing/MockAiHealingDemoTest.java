package com.cucumberbddparallel.examples.aihealing;

import com.cucumberbddparallel.examples.aihealing.support.DemoFixtureServer;
import com.cucumberbddparallel.examples.aihealing.support.MockLlmServer;
import com.cucumberbddparallel.framework.ai.AiConfig;
import com.cucumberbddparallel.framework.driver.DriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CI-safe proof: broken locator + mock OpenAI-compatible LLM → healing succeeds.
 */
class MockAiHealingDemoTest {

    private DemoFixtureServer fixtureServer;
    private MockLlmServer mockLlm;
    private WebDriver driver;

    @BeforeEach
    void setUp() throws Exception {
        fixtureServer = DemoFixtureServer.start(AiHealingSupport.fixtureHtml());
        mockLlm = MockLlmServer.start("#logo");
        AiHealingSupport.configureMockOpenAiProvider(mockLlm.baseUrl());
        assertTrue(AiConfig.isHealingEnabled(), "Healing should be on for this demo");
        driver = AiHealingSupport.startHeadlessChrome();
        driver.get(fixtureServer.baseUrl());
    }

    @AfterEach
    void tearDown() {
        DriverManager.quit();
        AiHealingSupport.clearAiHealingProperties();
        if (mockLlm != null) {
            mockLlm.stop();
        }
        if (fixtureServer != null) {
            fixtureServer.stop();
        }
    }

    @Test
    void brokenLogoLocatorHealsViaMockLlm() {
        DemoPage page = new DemoPage();
        page.assertLogoVisible();
        assertTrue(mockLlm.wasCalled(), "Mock LLM should have been called for healing");
    }

    @Test
    void sameLocatorFailsWhenHealingDisabled() {
        System.setProperty("ai.healing.enabled", "false");
        DemoPage page = new DemoPage();
        assertThrows(NoSuchElementException.class, page::assertLogoVisible);
        assertFalse(mockLlm.wasCalled(), "Mock LLM must not run when healing is off");
    }
}