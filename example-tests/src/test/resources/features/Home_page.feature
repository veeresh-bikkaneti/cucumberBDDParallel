Feature: Home page
  The example app's landing page - the starting point for every example in this module.

  Scenario: Logo and search box are displayed
    Given a user is on the example app home page
    Then the logo is displayed
    And the search box is displayed

  Scenario: Home page title is correct
    Given a user is on the example app home page
    Then the page title is "Example App - Home"
