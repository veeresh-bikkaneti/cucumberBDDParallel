package com.cucumberbddparallel.example.tables;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/** Step definitions for the tables example - thin wrappers around {@link TablesPage}. */
public class TablesSteps {

    private TablesPage page;

    /**
     * The page object is created here - not injected via the constructor. Cucumber builds
     * glue objects <em>before</em> any {@code @Before} hook runs, so constructor injection
     * would create the page before {@code Setup}'s {@code @Before(order = 0)} has opened
     * the browser. Default order (10000) runs after {@code Setup}'s order-0 hook.
     */
    @Before
    public void createPageObject() {
        this.page = new TablesPage();
    }

    @Given("a user is on the tables page")
    public void aUserIsOnTheTablesPage() {
        this.page.goToTablesPage();
    }

    @Then("the employees table has headers {string}, {string}, {string}")
    public void theEmployeesTableHasHeaders(String h1, String h2, String h3) {
        this.page.checkHeaders(h1, h2, h3);
    }

    @Then("the employees table has {int} data rows")
    public void theEmployeesTableHasDataRows(int expectedRows) {
        this.page.checkRowCount(expectedRows);
    }

    @When("the user sorts the employees table by salary")
    public void theUserSortsTheEmployeesTableBySalary() {
        this.page.sortBySalary();
    }

    @Then("the salary column is in ascending order")
    public void theSalaryColumnIsInAscendingOrder() {
        this.page.checkSalariesAscending();
    }
}
