#!/bin/bash
set -e

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
# Go to the project root (assuming script is in infrastructure/)
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

CERT_DIR="$PROJECT_ROOT/infrastructure/nginx/certs"
mkdir -p "$CERT_DIR"

if command -v mkcert &> /dev/null; then
    echo "✅ mkcert found. Generating trusted certificates..."
    mkcert -install
    mkcert -key-file "$CERT_DIR/localhost-key.pem" -cert-file "$CERT_DIR/localhost.pem" localhost 127.0.0.1 ::1
else
    echo "⚠️  mkcert not found. Generating self-signed certificates (Browser will warn)..."
    openssl req -x509 -newkey rsa:4096 -keyout "$CERT_DIR/localhost-key.pem" -out "$CERT_DIR/localhost.pem" -days 365 -nodes -subj '/CN=localhost'
fi

echo "Certificates generated in $CERT_DIR"