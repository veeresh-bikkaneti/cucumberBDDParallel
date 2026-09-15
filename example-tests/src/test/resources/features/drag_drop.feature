Feature: Drag and drop
  The framework's DragDropHelper fires the HTML5 drag-and-drop event sequence
  (dragstart → dragover → drop → dragend) with an injected DataTransfer,
  falling back to Selenium's Actions API only if JavaScript injection fails.

  Scenario: Dragging the source onto the target updates the status
    Given a user is on the drag and drop page
    When the user drags the source onto the target
    Then the drop status shows a successful drop
