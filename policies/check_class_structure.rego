package code.structure

# Generic check for class structure based on requirements
violation contains {"msg": msg} if {
    # input.reqs is an array because of --slurpfile in jq
    reqs_object := input.reqs[0]

    # Iterate over all requirement sets
    some req_set in reqs_object.requirements
    # Iterate over all requirements in the set
    some req in req_set.spec.requirements

    # Check if this requirement has a code validation for class structure
    req.code_validation
    req.code_validation.class_name

    # Get the file path and content
    file_path := req.code_validation.file_path
    # input.files is now an object/map: {"./path/to/file.java": "content"}
    # The object is the first element of the array from --slurpfile
    files_map := input.files[0]
    file_content := get_file_content(files_map, file_path)

    # If file is missing, that's a different check (implementation_validation)
    # Here we only check content if the file exists.
    file_content != ""

    # Iterate over required fields
    some field in req.code_validation.fields

    # Check if the field is missing from the file content
    not has_field(file_content, field.name, field.type)

    msg := sprintf("Requirement %s not fulfilled: Class '%s' in file '%s' is missing field '%s' with type '%s'.", [req.id, req.code_validation.class_name, file_path, field.name, field.type])
}

# Helper to get file content from the input map
get_file_content(files_map, path) = content if {
    # The path in the map includes the leading "./"
    content := files_map[sprintf("./%s", [path])]
} else = ""

# Helper to check for a field using regex
has_field(content, field_name, field_type) if {
    # Simple regex to find a private field declaration.
    # Example: "private int id;"
    # This is a simplification and might not cover all edge cases.
    pattern := sprintf(`private\s+%s\s+%s\s*;`, [field_type, field_name])
    regex.match(pattern, content)
}
