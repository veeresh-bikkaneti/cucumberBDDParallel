Feature: Drag and drop
  The framework's DragDropHelper wraps Selenium's Actions API, including the
  HTML5 drag-and-drop fallback for pages where native events don't fire.

  Scenario: Dragging the source onto the target updates the status
    Given a user is on the drag and drop page
    When the user drags the source onto the target
    Then the drop status shows a successful drop
