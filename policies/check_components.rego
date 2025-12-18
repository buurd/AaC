package structurizr.components

# REQ-006: WebServer must have a ProductController component
violation contains {"msg": "Requirement REQ-006 not fulfilled: WebServer is missing ProductController component."} if {
    # Find the WebServer container
    webshop := input.model.softwareSystems[_]
    webshop.name == "Webshop"

    webserver := webshop.containers[_]
    webserver.name == "WebServer"

    # Get components safely, defaulting to empty array
    components := object.get(webserver, "components", [])

    # Check if ProductController is missing
    not component_exists(components, "ProductController")
}

# Helper to check if a component with a given name exists in the components array
component_exists(components_array, component_name) if {
    some c in components_array
    c.name == component_name
}
