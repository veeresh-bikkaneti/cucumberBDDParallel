Feature: Search
  Searching from the home page shows a results page whose entries mention the query.

  Scenario Outline: Search results mention the query
    Given a user is on the example app home page
    When the user searches for "<query>"
    Then the results heading shows "<query>"
    And the first <count> result links contain "<query>"

    Examples:
      | query    | count |
      | cucumber | 3     |
      | selenium | 3     |
