package structurizr.containers

# Violation if Webshop system itself is missing
violation contains {"msg": "Webshop Software System (parent for containers) is missing from the model."} if {
    # Check if any software system named "Webshop" exists
    not webshop_exists
}

webshop_exists if {
    some s in input.model.softwareSystems
    s.name == "Webshop"
}

# REQ-003: Webshop must have a WebServer container
violation contains {"msg": "Requirement REQ-003 not fulfilled: Webshop is missing WebServer container."} if {
    some s in input.model.softwareSystems
    s.name == "Webshop"

    # Get containers safely, defaulting to empty array
    containers := object.get(s, "containers", [])

    # Check if WebServer is missing
    not container_exists(containers, "WebServer")
}

# REQ-004: Webshop must have a Database container
violation contains {"msg": "Requirement REQ-004 not fulfilled: Webshop is missing Database container."} if {
    some s in input.model.softwareSystems
    s.name == "Webshop"

    # Get containers safely, defaulting to empty array
    containers := object.get(s, "containers", [])

    # Check if Database is missing
    not container_exists(containers, "Database")
}

# Helper to check if a container with a given name exists in the containers array
container_exists(containers_array, container_name) if {
    some c in containers_array
    c.name == container_name
}
