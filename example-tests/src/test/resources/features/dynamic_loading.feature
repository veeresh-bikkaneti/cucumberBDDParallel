Feature: Dynamic loading
  Some content only appears after a delay - the framework's Wait helper keeps the
  test from racing ahead of the page.

  Scenario: Delayed content appears after clicking load
    Given a user is on the dynamic loading page
    When the user clicks the load button
    Then the delayed content appears showing "Content loaded dynamically"
