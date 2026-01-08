package code.structure

# Generic check for class structure based on requirements
violation contains {"msg": msg} if {
    reqs_object := input.reqs[0]
    files_map := input.files[0]

    some req_set in reqs_object.requirements
    some req in req_set.spec.requirements

    # Check if this requirement has a code validation
    req.code_validation

    file_path := req.code_validation.file_path
    file_content := get_file_content(files_map, file_path)

    # Check 1: File existence
    file_content == ""
    msg := sprintf("Requirement %s not fulfilled: Implementation file '%s' is missing.", [req.id, file_path])
}

violation contains {"msg": msg} if {
    reqs_object := input.reqs[0]
    files_map := input.files[0]

    some req_set in reqs_object.requirements
    some req in req_set.spec.requirements

    req.code_validation
    file_path := req.code_validation.file_path
    file_content := get_file_content(files_map, file_path)

    # Only check content if file exists
    file_content != ""

    # Check 2: Missing Fields
    req.code_validation.fields
    some field in req.code_validation.fields
    not has_field(file_content, field.name, field.type)
    msg := sprintf("Requirement %s not fulfilled: Class in file '%s' is missing field '%s' with type '%s'.", [req.id, file_path, field.name, field.type])
}

violation contains {"msg": msg} if {
    reqs_object := input.reqs[0]
    files_map := input.files[0]

    some req_set in reqs_object.requirements
    some req in req_set.spec.requirements

    req.code_validation
    file_path := req.code_validation.file_path
    file_content := get_file_content(files_map, file_path)

    # Only check content if file exists
    file_content != ""

    # Check 3: Missing Methods
    req.code_validation.method_name
    method := req.code_validation.method_name
    not has_method(file_content, method)
    msg := sprintf("Requirement %s not fulfilled: Class in file '%s' is missing method '%s'.", [req.id, file_path, method])
}

# Helper to get file content from the input map
get_file_content(files_map, path) = content if {
    # The path in the map includes the leading "./"
    content := files_map[sprintf("./%s", [path])]
} else = ""

# Helper to check for a field using regex
has_field(content, field_name, field_type) if {
    pattern := sprintf(`private\s+%s\s+%s\s*;`, [field_type, field_name])
    regex.match(pattern, content)
}

# Helper to check for a method using regex
has_method(content, method_name) if {
    # Matches "public/private/protected <Type> methodName("
    pattern := sprintf(`\s+%s\s*\(`, [method_name])
    regex.match(pattern, content)
}
