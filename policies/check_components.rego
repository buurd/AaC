package structurizr.components

# Generic check for component existence based on requirements
violation contains {"msg": msg} if {
    # input.reqs is an array because of --slurpfile in jq
    reqs_object := input.reqs[0]
    model := input.model[0].model

    # Iterate over all requirement sets
    some req_set in reqs_object.requirements
    # Iterate over all requirements in the set
    some req in req_set.spec.requirements

    # Check if this requirement has a model validation for a component
    req.model_validation
    req.model_validation.component
    req.model_validation.container

    # Find the container in the model
    # We search across all software systems
    some system in object.get(model, "softwareSystems", [])
    some container in object.get(system, "containers", [])
    container.name == req.model_validation.container

    # Get components safely, defaulting to empty array
    components := object.get(container, "components", [])

    # Check if the component is missing
    not component_exists(components, req.model_validation.component)

    msg := sprintf("Requirement %s failed: Component '%s' not found in container '%s'.", [req.id, req.model_validation.component, req.model_validation.container])
}

# Helper to check if a component with a given name exists in the components array
component_exists(components_array, component_name) if {
    some c in components_array
    c.name == component_name
}
