Feature: Login
  A plain form example: fill in credentials, submit, and check the welcome message.

  Scenario Outline: Logging in greets the user by name
    Given a user is on the login page
    When the user logs in as "<username>" with password "<password>"
    Then the welcome message shows "Welcome, <username>!"

    Examples:
      | username | password   |
      | alice    | wonderland |
      | bob      | builder    |
