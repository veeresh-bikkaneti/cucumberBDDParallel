package com.cucumberbddparallel.framework.driver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DriverManagerTest {

    @AfterEach
    void clearThreadLocal() {
        DriverManager.quit();
    }

    @Test
    void throwsWhenNothingHasBeenSetOnThisThread() {
        assertThrows(IllegalStateException.class, DriverManager::get);
    }

    @Test
    void getReturnsWhatWasSetOnTheSameThread() {
        WebDriver driver = mock(WebDriver.class);

        DriverManager.set(driver);

        assertSame(driver, DriverManager.get());
    }

    @Test
    void differentThreadsSeeDifferentDrivers() throws InterruptedException {
        WebDriver mainThreadDriver = mock(WebDriver.class);
        DriverManager.set(mainThreadDriver);

        AtomicReference<WebDriver> otherThreadDriver = new AtomicReference<>();
        AtomicReference<Throwable> childFailure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Thread other = new Thread(() -> {
            try {
                WebDriver driver = mock(WebDriver.class);
                DriverManager.set(driver);
                otherThreadDriver.set(DriverManager.get());
            } catch (Throwable t) {
                // A failure in the child thread would otherwise vanish silently while the
                // main thread happily asserts on a null reference - capture and rethrow it.
                childFailure.set(t);
            } finally {
                done.countDown();
            }
        });
        other.start();
        assertTrue(done.await(5, TimeUnit.SECONDS), "child thread did not finish in time");
        if (childFailure.get() != null) {
            throw new AssertionError("child thread failed", childFailure.get());
        }

        assertNotSame(mainThreadDriver, otherThreadDriver.get());
        assertSame(mainThreadDriver, DriverManager.get());
    }

    @Test
    void quitCallsDriverQuit() {
        WebDriver driver = mock(WebDriver.class);
        DriverManager.set(driver);

        DriverManager.quit();

        verify(driver).quit();
    }

    @Test
    void getThrowsAfterQuit() {
        DriverManager.set(mock(WebDriver.class));

        DriverManager.quit();

        assertThrows(IllegalStateException.class, DriverManager::get);
    }

    @Test
    void quitWithNothingSetIsANoOp() {
        assertDoesNotThrow(DriverManager::quit);
    }
}
