Feature: Tables
  The framework's TableHelper reads headers, rows, and individual cells without
  hand-rolled XPath - and verifies a client-side sort actually sorted.

  Scenario: Employee table has the expected structure
    Given a user is on the tables page
    Then the employees table has headers "Name", "Department", "Salary"
    And the employees table has 5 data rows

  Scenario: Sorting by salary orders rows ascending
    Given a user is on the tables page
    When the user sorts the employees table by salary
    Then the salary column is in ascending order
