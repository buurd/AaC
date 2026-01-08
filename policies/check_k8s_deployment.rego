package k8s.deployment

# Violation: Missing required deployment
violation contains {"msg": msg} if {
    # Find the requirement with k8s_validation
    req_set := input.reqs.requirements[_]
    req := req_set.spec.requirements[_]
    validation := req.k8s_validation
    required_deployment := validation.required_deployments[_]

    # Check if this deployment exists in the k8s manifests
    not deployment_exists(required_deployment)

    msg := sprintf("REQ-078: Missing required Kubernetes Deployment: '%s'", [required_deployment])
}

# Violation: Missing required service
violation contains {"msg": msg} if {
    req_set := input.reqs.requirements[_]
    req := req_set.spec.requirements[_]
    validation := req.k8s_validation
    required_service := validation.required_services[_]

    not service_exists(required_service)

    msg := sprintf("REQ-078: Missing required Kubernetes Service: '%s'", [required_service])
}

# Helper to check if a deployment exists
deployment_exists(name) if {
    resource := input.k8s[_]
    resource.kind == "Deployment"
    resource.metadata.name == name
}

# Helper to check if a DaemonSet exists (treating it as a deployment for validation purposes if needed, or separate)
deployment_exists(name) if {
    resource := input.k8s[_]
    resource.kind == "DaemonSet"
    resource.metadata.name == name
}

# Helper to check if a service exists
service_exists(name) if {
    resource := input.k8s[_]
    resource.kind == "Service"
    resource.metadata.name == name
}
