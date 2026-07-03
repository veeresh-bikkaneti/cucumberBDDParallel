package com.cucumberbddparallel.framework.interaction;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads HTML table and simple data-grid structures without XPath gymnastics.
 */
public final class TableHelper {

    private final WebDriver driver;

    public TableHelper(WebDriver driver) {
        this.driver = driver;
    }

    public List<String> headerTexts(String tableCss) {
        WebElement table = driver.findElement(By.cssSelector(tableCss));
        List<WebElement> headers = table.findElements(By.cssSelector("thead th"));
        if (headers.isEmpty()) {
            headers = table.findElements(By.cssSelector("tr:first-child th, tr:first-child td"));
        }
        List<String> texts = new ArrayList<>();
        for (WebElement header : headers) {
            texts.add(header.getText().trim());
        }
        return texts;
    }

    public List<List<String>> bodyRows(String tableCss) {
        WebElement table = driver.findElement(By.cssSelector(tableCss));
        List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));
        List<List<String>> matrix = new ArrayList<>();
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.cssSelector("td"));
            List<String> values = new ArrayList<>();
            for (WebElement cell : cells) {
                values.add(cell.getText().trim());
            }
            matrix.add(values);
        }
        return matrix;
    }

    public String cellText(String tableCss, int rowIndex, int columnIndex) {
        return bodyRows(tableCss).get(rowIndex).get(columnIndex);
    }
}