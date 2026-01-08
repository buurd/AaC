package structurizr.relation

# --- VIOLATION RULES ---

# FAILURE CASE 1: Source element not found
violation contains {"msg": msg} if {
    # Iterate over all requirements that have a model_validation block
    req := input.reqs[0].requirements[_].spec.requirements[_]
    req.model_validation.source

    # Find all elements in the model
    model := input.model[0].model
    people := {p | p := object.get(model, "people", [])[_]; p.name == req.model_validation.source}
    systems := {s | s := object.get(model, "softwareSystems", [])[_]; s.name == req.model_validation.source}
    containers := {c | s := object.get(model, "softwareSystems", [])[_]; c := object.get(s, "containers", [])[_]; c.name == req.model_validation.source}
    components := {comp | s := object.get(model, "softwareSystems", [])[_]; c := object.get(s, "containers", [])[_]; comp := object.get(c, "components", [])[_]; comp.name == req.model_validation.source}
    all_matching_elements := people | systems | containers | components

    # Check if the source element exists
    count(all_matching_elements) == 0

    msg := sprintf("Requirement %s failed: Source element '%s' not found in the model.", [req.id, req.model_validation.source])
}

# FAILURE CASE 2: Destination element not found
violation contains {"msg": msg} if {
    req := input.reqs[0].requirements[_].spec.requirements[_]
    req.model_validation.destination

    model := input.model[0].model
    people := {p | p := object.get(model, "people", [])[_]; p.name == req.model_validation.destination}
    systems := {s | s := object.get(model, "softwareSystems", [])[_]; s.name == req.model_validation.destination}
    containers := {c | s := object.get(model, "softwareSystems", [])[_]; c := object.get(s, "containers", [])[_]; c.name == req.model_validation.destination}
    components := {comp | s := object.get(model, "softwareSystems", [])[_]; c := object.get(s, "containers", [])[_]; comp := object.get(c, "components", [])[_]; comp.name == req.model_validation.destination}
    all_matching_elements := people | systems | containers | components

    count(all_matching_elements) == 0

    msg := sprintf("Requirement %s failed: Destination element '%s' not found in the model.", [req.id, req.model_validation.destination])
}

# FAILURE CASE 3: Relationship not found
violation contains {"msg": msg} if {
    req := input.reqs[0].requirements[_].spec.requirements[_]
    req.model_validation.source
    req.model_validation.destination

    model := input.model[0].model

    # Find source elements
    source_people := {p | p := object.get(model, "people", [])[_]; p.name == req.model_validation.source}
    source_systems := {s | s := object.get(model, "softwareSystems", [])[_]; s.name == req.model_validation.source}
    source_containers := {c | s := object.get(model, "softwareSystems", [])[_]; c := object.get(s, "containers", [])[_]; c.name == req.model_validation.source}
    source_components := {comp | s := object.get(model, "softwareSystems", [])[_]; c := object.get(s, "containers", [])[_]; comp := object.get(c, "components", [])[_]; comp.name == req.model_validation.source}
    source_elements := source_people | source_systems | source_containers | source_components

    # Find destination elements
    dest_people := {p | p := object.get(model, "people", [])[_]; p.name == req.model_validation.destination}
    dest_systems := {s | s := object.get(model, "softwareSystems", [])[_]; s.name == req.model_validation.destination}
    dest_containers := {c | s := object.get(model, "softwareSystems", [])[_]; c := object.get(s, "containers", [])[_]; c.name == req.model_validation.destination}
    dest_components := {comp | s := object.get(model, "softwareSystems", [])[_]; c := object.get(s, "containers", [])[_]; comp := object.get(c, "components", [])[_]; comp.name == req.model_validation.destination}
    dest_elements := dest_people | dest_systems | dest_containers | dest_components

    # This rule only triggers if both elements are found
    count(source_elements) > 0
    count(dest_elements) > 0

    source := source_elements[_]
    dest := dest_elements[_]

    not has_relationship(source, dest.id, req.model_validation.description_contains)

    msg := sprintf("Requirement %s failed: Relationship from '%s' to '%s' (description containing '%s') not found.", [
        req.id,
        req.model_validation.source,
        req.model_validation.destination,
        req.model_validation.description_contains
    ])
}

# Helper to check for a relationship
has_relationship(source, dest_id, desc) if {
    rel := object.get(source, "relationships", [])[_]
    rel.destinationId == dest_id
    contains(rel.description, desc)
}
