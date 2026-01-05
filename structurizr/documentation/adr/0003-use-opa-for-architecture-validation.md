# 3. Use Open Policy Agent (OPA) for Architecture Validation

Date: 2024-06-01

## Status
Accepted

## Context
In an "Architecture as Code" approach, simply defining the architecture is not enough; we must ensure the implementation adheres to it. We need a flexible, powerful engine to write rules that validate the relationships between Requirements, Architecture Model, and Code.

## Decision
We will use **Open Policy Agent (OPA)** and its query language **Rego** to implement our validation logic.

## Consequences
### Positive
*   **Flexibility**: Rego allows us to write complex logic (e.g., recursive traceability checks, cross-referencing JSON and YAML).
*   **Decoupling**: Validation rules (`policies/*.rego`) are separate from the validation script.
*   **Standardization**: OPA is an industry standard for policy-as-code.

### Negative
*   **Complexity**: Rego has a unique syntax that can be difficult to learn and debug.
