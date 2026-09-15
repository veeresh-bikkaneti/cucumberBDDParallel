package com.cucumberbddparallel.example.tables;

import com.cucumberbddparallel.example.support.AppServerHooks;
import com.cucumberbddparallel.framework.interaction.TableHelper;
import com.cucumberbddparallel.framework.page.BasePage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Page object for the tables demo ({@code /tables}). All table reading goes through the
 * framework {@link TableHelper} - the page object only knows <em>which</em> table and
 * <em>what</em> to assert about it.
 */
public class TablesPage extends BasePage {

    private static final Logger LOG = LoggerFactory.getLogger(TablesPage.class);
    private static final String EMPLOYEES_TABLE = "table#employees";
    private static final int SALARY_COLUMN = 2;

    private final TableHelper tables;

    @FindBy(css = "#sort-salary")
    private WebElement sortSalaryButton;

    /** Public so step classes can {@code new TablesPage()} in their {@code @Before} hooks. */
    public TablesPage() {
        this.tables = new TableHelper(driver);
    }

    void goToTablesPage() {
        driver.get(AppServerHooks.baseUrl() + "/tables");
        wait.forLoading(5);
    }

    void checkHeaders(String... expectedHeaders) {
        List<String> actual = this.tables.headerTexts(EMPLOYEES_TABLE);
        LOG.info("Table headers: {}", actual);
        Assert.assertEquals(actual, List.of(expectedHeaders), "Table headers did not match");
    }

    void checkRowCount(int expectedRows) {
        int actual = this.tables.bodyRows(EMPLOYEES_TABLE).size();
        LOG.info("Table data rows: expected {}, found {}", expectedRows, actual);
        Assert.assertEquals(actual, expectedRows, "Table row count did not match");
    }

    void sortBySalary() {
        wait.forElementToBeDisplayed(5, this.sortSalaryButton, "Sort by salary button").click();
    }

    /** Reads the salary column after sorting and asserts the values ascend. */
    void checkSalariesAscending() {
        List<List<String>> rows = this.tables.bodyRows(EMPLOYEES_TABLE);
        List<Long> salaries = new ArrayList<>();
        for (List<String> row : rows) {
            salaries.add(parseSalary(row.get(SALARY_COLUMN)));
        }
        LOG.info("Salaries after sort: {}", salaries);
        for (int i = 1; i < salaries.size(); i++) {
            Assert.assertTrue(salaries.get(i) >= salaries.get(i - 1),
                    "Salaries not ascending: " + salaries);
        }
    }

    /** Salaries render as plain numbers ("95000") - strip any non-digits before comparing, just in case the format gains currency symbols or separators later. */
    private static long parseSalary(String cellText) {
        String digits = cellText.replaceAll("[^0-9]", "");
        Assert.assertFalse(digits.isEmpty(), "Could not parse salary from \"" + cellText + "\"");
        return Long.parseLong(digits);
    }
}
