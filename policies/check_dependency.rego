package code.dependency

# Generic check for class dependencies based on requirements
violation contains {"msg": msg} if {
    # input.reqs is an array because of --slurpfile in jq
    reqs_object := input.reqs[0]

    # Iterate over all requirement sets
    some req_set in reqs_object.requirements
    # Iterate over all requirements in the set
    some req in req_set.spec.requirements

    # Check if this requirement has a code validation for dependency
    req.code_validation
    req.code_validation.source_file
    req.code_validation.dependency_class

    # Get the source file path and content
    source_file_path := req.code_validation.source_file
    source_file_content := get_file_content(input.files, source_file_path)

    # If file is missing, that's a different check (implementation_validation)
    # Here we only check content if the file exists.
    source_file_content != ""

    # Check if the dependency is missing from the file content
    not has_dependency(source_file_content, req.code_validation.dependency_class)

    msg := sprintf("Requirement %s not fulfilled: File '%s' is missing dependency on '%s'.", [req.id, source_file_path, req.code_validation.dependency_class])
}

# Helper to get file content from the input map
get_file_content(files_map, path) = content if {
    # The path in the map includes the leading "./"
    content := files_map[sprintf("./%s", [path])]
} else = ""

# Helper to check for a dependency using regex (Import check)
has_dependency(content, dependency_class) if {
    import_pattern := sprintf(`import .*\.%s;`, [dependency_class])
    regex.match(import_pattern, content)
}

# Helper to check for a dependency using regex (Field check)
has_dependency(content, dependency_class) if {
    # We don't strictly need 'private final' but it's good practice.
    # Let's make it a bit more flexible: just the type name followed by a variable name.
    # Example: "ProductRepository repository;"
    field_pattern := sprintf(`\b%s\s+\w+;`, [dependency_class])
    regex.match(field_pattern, content)
}

# Helper to check for a dependency using regex (Constructor arg check)
has_dependency(content, dependency_class) if {
    # Example: "public ProductController(ProductRepository repository)"
    ctor_pattern := sprintf(`\b%s\s+\w+`, [dependency_class])
    regex.match(ctor_pattern, content)
}
