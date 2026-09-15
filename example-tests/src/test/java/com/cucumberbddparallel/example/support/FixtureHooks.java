package com.cucumberbddparallel.example.support;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

/**
 * Starts the {@link FixtureServer} once per test JVM (before any scenario runs) and stops it
 * afterwards, publishing its base URL as the {@value #BASE_URL_PROPERTY} system property so
 * page objects can build fixture URLs without knowing the ephemeral port.
 *
 * This class must be on the Cucumber glue path - see the {@code glue} arrays in the
 * cucable template and the hand-written runners. Each Failsafe fork gets its own JVM and
 * therefore its own server instance on its own port, so parallel runs never clash.
 */
public class FixtureHooks {

    /** System property carrying the fixture server's base URL (e.g. {@code http://127.0.0.1:52341}). */
    public static final String BASE_URL_PROPERTY = "example.fixture.baseUrl";

    private static FixtureServer server;

    @BeforeAll
    public static void startFixtureServer() throws Exception {
        server = FixtureServer.start();
        System.setProperty(BASE_URL_PROPERTY, server.baseUrl());
    }

    @AfterAll
    public static void stopFixtureServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
        System.clearProperty(BASE_URL_PROPERTY);
    }

    /**
     * The fixture server's base URL. Fails fast with a clear message if the
     * {@code @BeforeAll} hook hasn't run (usually: this package missing from the glue path).
     */
    public static String baseUrl() {
        String baseUrl = System.getProperty(BASE_URL_PROPERTY);
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "Fixture server URL is not set - FixtureHooks' @BeforeAll hook did not run. "
                            + "Make sure 'com.cucumberbddparallel.example.support' is in the runner's glue.");
        }
        return baseUrl;
    }
}
