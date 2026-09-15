package com.cucumberbddparallel.framework.interaction;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads HTML table and simple data-grid structures without XPath gymnastics.
 */
public final class TableHelper {

    private static final Duration TABLE_WAIT = Duration.ofSeconds(10);

    private final WebDriver driver;

    public TableHelper(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "driver");
    }

    public List<String> headerTexts(String tableCss) {
        WebElement table = findTable(tableCss);
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
        WebElement table = findTable(tableCss);
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

    /**
     * Text of one cell. Validates the indices up front so a typo'd row/column fails with
     * "row 7 doesn't exist in table '#orders' (3 rows found)" instead of a bare
     * {@link IndexOutOfBoundsException} with no context about which table was involved.
     */
    public String cellText(String tableCss, int rowIndex, int columnIndex) {
        Objects.requireNonNull(tableCss, "tableCss");
        List<List<String>> rows = bodyRows(tableCss);
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IndexOutOfBoundsException(
                    "Row index " + rowIndex + " out of bounds for table '" + tableCss
                            + "' (" + rows.size() + " body rows found)");
        }
        List<String> row = rows.get(rowIndex);
        if (columnIndex < 0 || columnIndex >= row.size()) {
            throw new IndexOutOfBoundsException(
                    "Column index " + columnIndex + " out of bounds for row " + rowIndex
                            + " of table '" + tableCss + "' (" + row.size() + " columns found)");
        }
        return row.get(columnIndex);
    }

    // Waits for the table to actually be in the DOM before touching it - tables usually
    // render asynchronously, and findElement on a not-yet-rendered table is the classic
    // source of "works locally, flakes in CI."
    private WebElement findTable(String tableCss) {
        Objects.requireNonNull(tableCss, "tableCss");
        new WebDriverWait(driver, TABLE_WAIT)
                .withMessage("Table '" + tableCss + "' was not present after "
                        + TABLE_WAIT.toSeconds() + " seconds")
                .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(tableCss)));
        return driver.findElement(By.cssSelector(tableCss));
    }
}
