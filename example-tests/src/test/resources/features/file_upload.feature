Feature: File upload
  The framework's FileUploadHelper pushes a local file into a file input - the
  test creates a throwaway file on the fly, so nothing is checked into the repo.

  Scenario: Uploading a text file shows its name
    Given a user is on the file upload page
    When the user uploads a text file named "example-upload.txt"
    Then the upload result shows "example-upload.txt"
