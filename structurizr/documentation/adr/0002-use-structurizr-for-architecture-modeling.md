# 2. Use Structurizr for Architecture Modeling

Date: 2024-05-15

## Status
Accepted

## Context
We need a way to document our software architecture (C4 model) that keeps pace with the code. Traditional diagramming tools (Visio, Draw.io) produce static artifacts that quickly become outdated and cannot be automatically validated against the implementation.

## Decision
We will use **Structurizr DSL** to define our architecture as code. The `workspace.dsl` file will be the single source of truth for the software model.

## Consequences
### Positive
*   **Version Control**: The architecture is text-based and versioned in Git.
*   **Automation**: We can parse the DSL (export to JSON) to run automated validation checks (e.g., "Does this container exist in the code?").
*   **Multiple Views**: We can generate different views (System, Container, Component) from the same underlying model.

### Negative
*   **Learning Curve**: Requires learning the Structurizr DSL syntax.
*   **Visualization**: Requires a rendering tool (Structurizr Lite) to view the diagrams.
