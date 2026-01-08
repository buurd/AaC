package structurizr.containers

# Generic rule: Check if parent system exists
violation contains {"msg": msg} if {
    req := input.reqs[0].requirements[_].spec.requirements[_]
    req.model_validation.parent
    req.model_validation.container

    model := input.model[0].model
    systems := {s | s := object.get(model, "softwareSystems", [])[_]; s.name == req.model_validation.parent}

    count(systems) == 0
    msg := sprintf("Requirement %s failed: Parent Software System '%s' not found in the model.", [req.id, req.model_validation.parent])
}

# Generic rule: Check if container exists in parent
violation contains {"msg": msg} if {
    req := input.reqs[0].requirements[_].spec.requirements[_]
    req.model_validation.parent
    req.model_validation.container

    model := input.model[0].model
    systems := {s | s := object.get(model, "softwareSystems", [])[_]; s.name == req.model_validation.parent}

    count(systems) > 0
    system := systems[_]

    containers := object.get(system, "containers", [])
    matching_containers := {c | c := containers[_]; c.name == req.model_validation.container}

    count(matching_containers) == 0
    msg := sprintf("Requirement %s failed: Container '%s' not found in Software System '%s'.", [req.id, req.model_validation.container, req.model_validation.parent])
}
