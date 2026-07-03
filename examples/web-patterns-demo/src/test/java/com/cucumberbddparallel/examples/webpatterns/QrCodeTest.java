package com.cucumberbddparallel.examples.webpatterns;

import com.cucumberbddparallel.examples.webpatterns.support.WebPatternsFixtureServer;
import com.cucumberbddparallel.framework.driver.DriverManager;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrCodeTest {

    private WebPatternsFixtureServer server;
    private WebDriver driver;

    @BeforeEach
    void setUp() throws Exception {
        server = WebPatternsFixtureServer.start(WebPatternsSupport.fixturesRoot());
        driver = WebPatternsSupport.startHeadlessChrome(null);
        driver.get(server.url("/qr.html"));
    }

    @AfterEach
    void tearDown() {
        DriverManager.quit();
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void decodesQrPayloadFromScreenshot() throws Exception {
        WebElement qrImage = driver.findElement(By.id("qr-image"));
        byte[] png = qrImage.getScreenshotAs(OutputType.BYTES);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap);
        assertEquals(WebPatternsFixtureServer.QR_PAYLOAD, result.getText());
    }
}