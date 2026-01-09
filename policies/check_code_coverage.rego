package check_code_coverage

import rego.v1

# Default to allow, deny specific violations
default allow = false

# Violation if coverage report is missing
deny contains msg if {
    app := input.applications[_]
    not coverage_report_exists(app)
    msg := sprintf("Application '%s' is missing a code coverage report.", [app])
}

# Violation if coverage is below threshold
deny contains msg if {
    app := input.applications[_]
    coverage := get_coverage(app)
    coverage < 80
    msg := sprintf("Application '%s' has code coverage of %d%%, which is below the required 80%%.", [app, coverage])
}

# Helper to check if report exists (mock implementation for now, assumes input has this data)
coverage_report_exists(app) if {
    input.coverage_data[app]
}

# Helper to get coverage (mock implementation for now)
get_coverage(app) = coverage if {
    coverage := input.coverage_data[app].percentage
}
