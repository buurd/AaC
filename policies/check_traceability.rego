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
    # We need to handle both single string and array of strings
    not all_trace_targets_exist(req.tracesTo)

    msg := sprintf("Requirement %s traces to non-existent requirement: %s", [req.id, req.tracesTo])
}

has_trace(req) if {
    req.tracesTo
    req.tracesTo != ""
    count(req.tracesTo) > 0
}

# Helper to check if all targets exist
all_trace_targets_exist(targets) if {
    is_array(targets)
    # Check that for every target in the array, it exists
    count({t | t := targets[_]; trace_target_exists(t)}) == count(targets)
}

all_trace_targets_exist(target) if {
    is_string(target)
    trace_target_exists(target)
}

trace_target_exists(target_id) if {
    some req_set in input.requirements
    some other_req in req_set.spec.requirements
    other_req.id == target_id
}
