package com.cucumberbddparallel.example.support;

import com.cucumberbddparallel.examples.app.ExampleAppServer;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

/**
 * Starts the {@link ExampleAppServer} before scenarios run and stops it afterwards,
 * publishing its base URL as the {@value #BASE_URL_PROPERTY} system property so page
 * objects can build app URLs without knowing the ephemeral port.
 *
 * <p>Scope note: Cucumber's {@code @BeforeAll} runs once per Cucumber execution, and each
 * Cucable-generated runner is its own execution - so the server starts and stops once per
 * feature file, not once per JVM. That is safe because TestNG runs the generated runners
 * sequentially within a fork, and each Failsafe fork is its own JVM with its own server
 * instance on its own ephemeral port, so parallel runs never clash.
 *
 * <p>This class must be on the Cucumber glue path - see the {@code glue} arrays in the
 * cucable template and the hand-written runners.
 */
public class AppServerHooks {

    /** System property carrying the app server's base URL (e.g. {@code http://127.0.0.1:52341}). */
    public static final String BASE_URL_PROPERTY = "example.fixture.baseUrl";

    private static ExampleAppServer server;

    @BeforeAll
    public static void startAppServer() {
        try {
            server = ExampleAppServer.start();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not start the example app server", e);
        }
        System.setProperty(BASE_URL_PROPERTY, server.baseUrl());
    }

    @AfterAll
    public static void stopAppServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
        System.clearProperty(BASE_URL_PROPERTY);
    }

    /**
     * The app server's base URL. Fails fast with a clear message if the
     * {@code @BeforeAll} hook hasn't run (usually: this package missing from the glue path).
     */
    public static String baseUrl() {
        String baseUrl = System.getProperty(BASE_URL_PROPERTY);
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "App server URL is not set - AppServerHooks' @BeforeAll hook did not run. "
                            + "Make sure 'com.cucumberbddparallel.example.support' is in the runner's glue.");
        }
        return baseUrl;
    }
}
