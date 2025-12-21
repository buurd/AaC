package security.https

violation contains {"msg": msg, "file": file_path} if {
    some file_path, content in input.files

    # Check for http:// usage
    # We use regex to find http:// but not https://
    # regex: http://[^s]
    # But simpler: contains(content, "http://")
    contains(content, "http://")

    msg := sprintf("File '%s' contains insecure URL 'http://'. Use 'https://' instead.", [file_path])
}
