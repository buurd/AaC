package requirements.traceability

# Check that non-context requirements trace to a parent requirement
violation contains {"msg": msg} if {
    some req_set in input.requirements
    some req in req_set.spec.requirements

    # Skip context level requirements
    req.c4_level != "context"

    # Check if tracesTo is missing or empty
    not has_trace(req)

    msg := sprintf("Requirement %s (Level: %s) is missing a 'tracesTo' reference.", [req.id, req.c4_level])
}

# Check that the traced requirement actually exists
violation contains {"msg": msg} if {
    some req_set in input.requirements
    some req in req_set.spec.requirements

    # Skip context level requirements
    req.c4_level != "context"

    # Only check if trace exists
    has_trace(req)

    # Check if the target requirement exists
    not trace_target_exists(req.tracesTo)

    msg := sprintf("Requirement %s traces to non-existent requirement: %s", [req.id, req.tracesTo])
}

has_trace(req) if {
    req.tracesTo
    req.tracesTo != ""
}

trace_target_exists(target_id) if {
    some req_set in input.requirements
    some other_req in req_set.spec.requirements
    other_req.id == target_id
}