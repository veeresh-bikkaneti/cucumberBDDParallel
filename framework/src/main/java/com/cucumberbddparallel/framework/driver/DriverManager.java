package com.cucumberbddparallel.framework.driver;

import org.openqa.selenium.WebDriver;

/**
 * Holds "the current test's WebDriver" so page objects don't have to pass one around
 * through every constructor.
 *
 * It's a {@link ThreadLocal} because this framework is built for parallel test execution -
 * if two scenarios run at the same time on different threads, each needs its own browser,
 * and neither should be able to accidentally reach into the other's. A plain static field
 * would let one thread's driver leak into another thread's test by mistake, which is exactly
 * the kind of bug that's miserable to track down in a parallel suite.
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
    }

    /** The current thread's driver. Throws if nothing has called {@link #set} yet on this thread. */
    public static WebDriver get() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("No WebDriver set up for this thread - did the @Before hook run?");
        }
        return driver;
    }

    /** Called by {@link Setup}'s {@code @Before} hook once a browser session has been opened. */
    public static void set(WebDriver driver) {
        DRIVER.set(driver);
    }

    /** Closes the current thread's browser (if any) and clears the thread-local so it can't leak into the next test. */
    public static void quit() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}
