Feature: Document Tasks

  Scenario: CRUD Document Tasks
    When GET document-tasks
    Then the response code is 200
    When POST document-task for for document with no annotations
    Then the response code is 400
