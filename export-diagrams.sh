#!/bin/bash
set -e

# Output directory for diagrams
OUTPUT_DIR="documentation/diagrams"
mkdir -p "$OUTPUT_DIR"

echo "--- Exporting Structurizr Views to PlantUML ---"

# 1. Export DSL to PlantUML using Structurizr CLI
# We mount the current directory to /root/workspace and set it as the working directory
docker run --rm \
  -v "$(pwd):/root/workspace" \
  -w /root/workspace \
  structurizr/cli \
  export -workspace structurizr/workspace.dsl -format plantuml -output "$OUTPUT_DIR"

echo "--- Converting PlantUML to PNG ---"

# 2. Convert PlantUML to PNG using PlantUML Server/CLI
# We use the plantuml/plantuml image to process the files
# Note: The plantuml image might run as a non-root user, so we ensure the directory is writable or just rely on standard behavior.
docker run --rm \
  -v "$(pwd)/$OUTPUT_DIR:/data" \
  plantuml/plantuml \
  -tpng "/data/*.puml"

# Cleanup .puml files if you only want images
# rm "$OUTPUT_DIR"/*.puml

echo "✅ Diagrams exported to $OUTPUT_DIR"
ls -1 "$OUTPUT_DIR"/*.png 2>/dev/null || echo "No PNG files found. Check for errors above."
