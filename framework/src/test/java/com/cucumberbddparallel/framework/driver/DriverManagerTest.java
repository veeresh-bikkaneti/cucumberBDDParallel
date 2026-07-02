package com.cucumberbddparallel.framework.driver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

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
        CountDownLatch done = new CountDownLatch(1);
        Thread other = new Thread(() -> {
            WebDriver driver = mock(WebDriver.class);
            DriverManager.set(driver);
            otherThreadDriver.set(DriverManager.get());
            done.countDown();
        });
        other.start();
        done.await();

        assertNotSame(mainThreadDriver, otherThreadDriver.get());
        assertSame(mainThreadDriver, DriverManager.get());
    }
}
