package com.cucumberbddparallel.example.dragdrop;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/** Step definitions for the drag-and-drop example - thin wrappers around {@link DragDropPage}. */
public class DragDropSteps {

    private DragDropPage page;

    /**
     * The page object is created here - not injected via the constructor. Cucumber builds
     * glue objects <em>before</em> any {@code @Before} hook runs, so constructor injection
     * would create the page before {@code Setup}'s {@code @Before(order = 0)} has opened
     * the browser. Default order (10000) runs after {@code Setup}'s order-0 hook.
     */
    @Before
    public void createPageObject() {
        this.page = new DragDropPage();
    }

    @Given("a user is on the drag and drop page")
    public void aUserIsOnTheDragAndDropPage() {
        this.page.goToDragDropPage();
    }

    @When("the user drags the source onto the target")
    public void theUserDragsTheSourceOntoTheTarget() {
        this.page.dragSourceOntoTarget();
    }

    @Then("the drop status shows a successful drop")
    public void theDropStatusShowsASuccessfulDrop() {
        this.page.checkDropSucceeded();
    }
}
