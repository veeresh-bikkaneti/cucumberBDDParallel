package com.cucumberbddparallel.examples.webpatterns;

import com.cucumberbddparallel.examples.webpatterns.support.WebPatternsFixtureServer;
import com.cucumberbddparallel.framework.driver.DriverManager;
import com.cucumberbddparallel.framework.interaction.TableHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TablePatternsTest {

    private WebPatternsFixtureServer server;
    private WebDriver driver;

    @BeforeEach
    void setUp() throws Exception {
        server = WebPatternsFixtureServer.start(WebPatternsSupport.fixturesRoot());
        driver = WebPatternsSupport.startHeadlessChrome(null);
        driver.get(server.url("/tables.html"));
    }

    @AfterEach
    void tearDown() {
        DriverManager.quit();
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void readsGridHeadersAndRows() {
        TableHelper table = new TableHelper(driver);
        assertEquals(List.of("Order ID", "Customer", "Status"), table.headerTexts("#orders"));
        List<List<String>> rows = table.bodyRows("#orders");
        assertEquals(3, rows.size());
        assertEquals("ORD-1002", table.cellText("#orders", 1, 0));
        assertTrue(rows.get(2).contains("Delivered"));
    }
}