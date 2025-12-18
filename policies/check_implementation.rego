package project_files

# Generic check for all implementation requirements
violation contains {"msg": msg} if {
    # input.reqs is an array because of --slurpfile in jq
    # The actual requirements object is the first element
    reqs_object := input.reqs[0]

    # Iterate over all requirement sets
    some req_set in reqs_object.requirements
    # Iterate over all requirements in the set
    some req in req_set.spec.requirements

    # Check if this requirement has an implementation validation
    req.implementation_validation

    # Get the file path from the requirement
    file_to_check := req.implementation_validation.file_path

    # Check if the file exists in the list of project files
    not file_exists(input.files, file_to_check)

    msg := sprintf("Requirement %s not fulfilled: Implementation file '%s' is missing.", [req.id, file_to_check])
}

# Helper to check if a file exists in the list
# input.files is also an array of arrays because of --slurpfile
file_exists(files_input, path) if {
    # files_input is [ ["file1", "file2", ...] ]
    file_list := files_input[0]
    some f in file_list
    f == path
}
