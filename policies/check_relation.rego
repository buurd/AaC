package structurizr

violation contains {"msg": "Requirement REQ-001 not fulfilled: Customer does not use Webshop"} if {
    # Find the customer
    customer := input.model.people[_]
    customer.name == "Customer"

    # Find the webshop
    webshop := input.model.softwareSystems[_]
    webshop.name == "Webshop"

    # Check that the relationship does NOT exist
    not has_uses_relationship(customer, webshop.id)
}

has_uses_relationship(source_element, dest_id) if {
    some rel in source_element.relationships
    rel.destinationId == dest_id
    rel.description == "Uses"
}
