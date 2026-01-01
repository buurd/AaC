package integration.pact

# Check if a requirement specifies a Pact verification and if that Pact file exists
violation contains {"msg": msg} if {
    # input.reqs is an array because of --slurpfile in jq
    reqs_object := input.reqs[0]

    # Iterate over all requirement sets
    some req_set in reqs_object.requirements
    # Iterate over all requirements in the set
    some req in req_set.spec.requirements

    # Check if this requirement has a pact_validation block
    req.pact_validation

    # Handle both single object and array of objects
    some validation in get_pact_validations(req.pact_validation)

    validation.provider
    validation.consumer

    # Construct the expected Pact file name
    # Standard Pact naming: Consumer-Provider.json
    pact_file_name := sprintf("pacts/%s-%s.json", [validation.consumer, validation.provider])

    # Check if the file exists in the list of project files
    not file_exists(input.files, pact_file_name)

    msg := sprintf("Requirement %s not fulfilled: Pact contract file '%s' is missing.", [req.id, pact_file_name])
}

# Helper to normalize pact_validation to a list
get_pact_validations(val) = [val] if {
    is_object(val)
}

get_pact_validations(val) = val if {
    is_array(val)
}

# Helper to check if a file exists in the list
# input.files is also an array of arrays because of --slurpfile
file_exists(files_input, path) if {
    # files_input is [ ["file1", "file2", ...] ]
    file_list := files_input[0]
    some f in file_list
    f == path
}
