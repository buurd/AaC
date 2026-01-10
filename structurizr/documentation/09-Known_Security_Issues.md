# Known Security Issues

This document lists the known security issues identified in the system components. The issues are sourced from automated vulnerability scans (Trivy).

**Last Updated:** 2026-01-10

## Summary

| Component | Image / Version | Critical Vulnerabilities | High Vulnerabilities | Status |
| :--- | :--- | :---: | :---: | :--- |
| **Promtail** | `grafana/promtail:3.3.3` | 0 | 7 | ✅ Resolved (Criticals) |
| **Loki** | `grafana/loki:3.3.2` | 0 | 0 | ✅ Resolved (Criticals) |
| **Keycloak** | `quay.io/keycloak/keycloak:24.0.0` | 0 | 7 | ✅ Resolved (Criticals) |
| **PostgreSQL** | `postgres:15` | 0 | 8 | 🔴 Unresolved |
| **Webshop** | `pom.xml` | 0 | 1 | 🔴 Unresolved |
| **Product Management** | `pom.xml` | 0 | 1 | 🔴 Unresolved |
| **Order Service** | `pom.xml` | 0 | 1 | 🔴 Unresolved |

---

## Detailed Issues (Top Severe)

### 1. Promtail - GnuPG Information Disclosure
*   **Component:** Promtail
*   **Image:** `grafana/promtail:3.3.3`
*   **Library:** `gpgv` (version `2.4.4-2ubuntu17`)
*   **Vulnerability:** CVE-2025-68973
*   **Severity:** HIGH
*   **Description:** GnuPG information disclosure and potential arbitrary code execution via out-of-bounds write.
*   **Status:** Fixed in `2.4.4-2ubuntu17.4`.
*   **Solution:** Upgrade base image.

### 2. Promtail - JWT-Go Excessive Memory Allocation
*   **Component:** Promtail
*   **Image:** `grafana/promtail:3.3.3`
*   **Library:** `github.com/golang-jwt/jwt/v5` (version `v5.2.1`)
*   **Vulnerability:** CVE-2025-30204
*   **Severity:** HIGH
*   **Description:** `jwt-go` allows excessive memory allocation during header parsing.
*   **Status:** Fixed in `5.2.2`.
*   **Solution:** Upgrade Promtail.

### 3. PostgreSQL - GnuPG Information Disclosure
*   **Component:** PostgreSQL
*   **Image:** `postgres:15`
*   **Library:** `dirmngr` (version `2.4.7-21+b3`)
*   **Vulnerability:** CVE-2025-68973
*   **Severity:** HIGH
*   **Description:** GnuPG information disclosure and potential arbitrary code execution via out-of-bounds write.
*   **Status:** Affected.
*   **Solution:** Upgrade PostgreSQL image.

### 4. Applications - Jackson Core StackOverflowError
*   **Component:** Webshop, Product Management, Order Service
*   **Library:** `com.fasterxml.jackson.core:jackson-core` (version `2.14.2`)
*   **Vulnerability:** CVE-2025-52999
*   **Severity:** HIGH
*   **Description:** Potential `StackOverflowError` in `jackson-core`.
*   **Status:** Fixed in `2.15.0`.
*   **Solution:** Upgrade Jackson dependency in `pom.xml` to `2.15.0` or later.

### 5. Java Runtime - GnuPG Information Disclosure
*   **Component:** Java Runtime
*   **Image:** `eclipse-temurin:21-jre`
*   **Library:** `dirmngr` (version `2.4.4-2ubuntu17.3`)
*   **Vulnerability:** CVE-2025-68973
*   **Severity:** HIGH
*   **Description:** GnuPG information disclosure and potential arbitrary code execution via out-of-bounds write.
*   **Status:** Fixed in `2.4.4-2ubuntu17.4`.
*   **Solution:** Upgrade base image.

### 6. Keycloak - OpenJDK Glyph Drawing
*   **Component:** Keycloak
*   **Image:** `quay.io/keycloak/keycloak:24.0.0`
*   **Library:** `java-17-openjdk-headless` (version `1:17.0.14.0.7-2.el9`)
*   **Vulnerability:** CVE-2025-30749
*   **Severity:** HIGH
*   **Description:** OpenJDK Better Glyph drawing (Oracle CPU 2025-07).
*   **Status:** Fixed in `1:17.0.16.0.8-2.el9`.
*   **Solution:** Upgrade Keycloak base image.

### 7. Keycloak - Netty HTTP/2 DDoS
*   **Component:** Keycloak
*   **Image:** `quay.io/keycloak/keycloak:24.0.0`
*   **Library:** `io.netty:netty-codec-http2` (version `4.1.106.Final`)
*   **Vulnerability:** CVE-2025-55163
*   **Severity:** HIGH
*   **Description:** Netty MadeYouReset HTTP/2 DDoS Vulnerability.
*   **Status:** Fixed in `4.2.4.Final`.
*   **Solution:** Upgrade Keycloak.

### 8. Keycloak - Quarkus HTTP Cookie Smuggling
*   **Component:** Keycloak
*   **Image:** `quay.io/keycloak/keycloak:24.0.0`
*   **Library:** `io.quarkus.http:quarkus-http-core` (version `5.0.3.Final`)
*   **Vulnerability:** CVE-2024-12397
*   **Severity:** HIGH
*   **Description:** Quarkus HTTP Cookie Smuggling.
*   **Status:** Fixed in `5.3.4`.
*   **Solution:** Upgrade Keycloak.

### 9. Keycloak - Quarkus Config Leak
*   **Component:** Keycloak
*   **Image:** `quay.io/keycloak/keycloak:24.0.0`
*   **Library:** `io.quarkus:quarkus-core` (version `3.8.1`)
*   **Vulnerability:** CVE-2024-2700
*   **Severity:** HIGH
*   **Description:** Leak of local configuration properties into Quarkus applications.
*   **Status:** Fixed in `3.9.2`.
*   **Solution:** Upgrade Keycloak.

### 10. Keycloak - SAML Privilege Escalation
*   **Component:** Keycloak
*   **Image:** `quay.io/keycloak/keycloak:24.0.0`
*   **Library:** `org.keycloak:keycloak-saml-core` (version `23.0.7`)
*   **Vulnerability:** CVE-2024-8698
*   **Severity:** HIGH
*   **Description:** Improper Verification of SAML Responses Leading to Privilege Escalation.
*   **Status:** Fixed in `24.0.8`.
*   **Solution:** Upgrade Keycloak.

### 11. Promtail - Go Crypto SSH Denial of Service
*   **Component:** Promtail
*   **Image:** `grafana/promtail:3.3.3`
*   **Library:** `golang.org/x/crypto` (version `v0.32.0`)
*   **Vulnerability:** CVE-2025-22869
*   **Severity:** HIGH
*   **Description:** Denial of Service in the Key Exchange of `golang.org/x/crypto/ssh`.
*   **Status:** Fixed in `0.35.0`.
*   **Solution:** Upgrade Promtail.

### 12. Promtail - Go Archive/Tar Unbounded Allocation
*   **Component:** Promtail
*   **Image:** `grafana/promtail:3.3.3`
*   **Library:** `stdlib` (Go Runtime `v1.23.6`)
*   **Vulnerability:** CVE-2025-58183
*   **Severity:** HIGH
*   **Description:** Unbounded allocation when parsing GNU sparse map.
*   **Status:** Fixed in `1.24.8`.
*   **Solution:** Upgrade Promtail.

### 13. Keycloak - JDBC Driver Improper Input Validation
*   **Component:** Keycloak
*   **Image:** `quay.io/keycloak/keycloak:24.0.0`
*   **Library:** `com.microsoft.sqlserver:mssql-jdbc` (version `12.2.0.jre11`)
*   **Vulnerability:** CVE-2025-59250
*   **Severity:** HIGH
*   **Description:** JDBC Driver for SQL Server has improper input validation issue.
*   **Status:** Fixed in `12.2.1.jre11`.
*   **Solution:** Upgrade Keycloak.

### 14. Promtail - Go OAuth2 Memory Consumption
*   **Component:** Promtail
*   **Image:** `grafana/promtail:3.3.3`
*   **Library:** `golang.org/x/oauth2` (version `v0.23.0`)
*   **Vulnerability:** CVE-2025-22868
*   **Severity:** HIGH
*   **Description:** Unexpected memory consumption during token parsing.
*   **Status:** Fixed in `0.27.0`.
*   **Solution:** Upgrade Promtail.

### 15. Keycloak - Keycloak Sensitive Data Exposure
*   **Component:** Keycloak
*   **Image:** `quay.io/keycloak/keycloak:24.0.0`
*   **Library:** `org.keycloak:keycloak-quarkus-server` (version `24.0.0`)
*   **Vulnerability:** CVE-2024-10451
*   **Severity:** HIGH
*   **Description:** Sensitive Data Exposure in Keycloak Build Process.
*   **Status:** Fixed in `24.0.9`.
*   **Solution:** Upgrade Keycloak.

---

## How to Update This Document

1.  **Run Vulnerability Scan**: Execute the `scan-vulnerabilities.sh` script in the project root. This will generate a report at `logs/trivy-report.txt`.
    ```bash
    ./scan-vulnerabilities.sh
    ```
2.  **Analyze Report**: Open `logs/trivy-report.txt` and look for vulnerabilities with `CRITICAL` or `HIGH` severity.
3.  **Update Summary**: Update the "Summary" table in this document with the current counts of Critical and High vulnerabilities for each component. Mark the status as "Resolved" if counts are zero, or "Unresolved" otherwise.
4.  **Update Detailed Issues**:
    *   Identify the top 10-15 most severe issues (prioritize Critical over High).
    *   For each issue, extract the Component, Image, Library, CVE ID, Severity, Description, Status (Fixed/Affected), and Solution (Fixed Version).
    *   Replace the existing list with the new findings.
5.  **Commit**: Save the changes to this file.
