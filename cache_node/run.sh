#!/bin/sh

# Function to display usage instructions
usage() {
    echo "Usage: $0 <port>"
    echo "  <port>  The port number to expose and map (1025-65535)"
    exit 1
}

# Check if a port number is provided
if [ -z "$1" ]; then
    echo "Error: No port specified."
    usage
fi

PORT=$1

echo "Building Docker image..."

# Build the Docker image using Gradle
./gradlew dockerImage

# Check if the Docker build was successful
if [ $? -ne 0 ]; then
    echo "Error: Docker image build failed."
    exit 1
fi

# Retrieve the current Git commit hash for tagging
COMMIT_HASH=$(git rev-parse HEAD)

# Define the Docker image tag
IMAGE_TAG="cache-node:0.0.1_$COMMIT_HASH"

echo "Running Docker container on port $PORT..."

# Run the Docker container with the specified port
docker run -e PORT="$PORT" -p "$PORT":"$PORT" "$IMAGE_TAG"
