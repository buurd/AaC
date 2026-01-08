# 7. Use Pact for Contract Testing

Date: 2024-07-01

## Status
Accepted

## Context
In a distributed microservices architecture, integration testing is difficult. End-to-end tests are slow and flaky. We need a way to ensure that Service A (Consumer) and Service B (Provider) speak the same language without spinning up the entire world.

## Decision
We will use **Pact** for Consumer-Driven Contract Testing. Consumers define their expectations (Contracts), and Providers verify they meet those expectations during their build.

## Consequences
### Positive
*   **Fast Feedback**: Issues are caught at the unit/integration test level, not in E2E tests.
*   **Decoupling**: Teams can develop independently as long as they adhere to the contract.
*   **Documentation**: The Pact files serve as up-to-date API documentation.

### Negative
*   **Maintenance**: Contracts must be maintained and versioned.
*   **Workflow**: Requires a workflow to share contracts (Pact Broker or shared file system). We are currently using the file system (`/pacts` folder).
