# Cache Node Application

This application is a gRPC-based cache node service. It is packaged as a Docker container and can be run using the provided `run.sh` script.

## Prerequisites

- Docker must be installed and running on your system.
- Ensure you have the necessary permissions to execute shell scripts.

## Usage

The `run.sh` script is used to build and run the Docker container for the cache node application. It requires a port number as an argument.

### Running the Script

```bash
./run.sh <port>
```

- `<port>`: The port number on which the cache node service will listen. It must be a valid port number between 1025 and 65535.

### Example

To run the cache node service on port 50051, execute:

```bash
./run.sh 50051
```

This will build the Docker image and run the container, exposing the specified port.

## Script Details

- The script builds the Docker image using Gradle.
- It tags the Docker image with the current Git commit hash.
- It runs the Docker container, mapping the specified port.

## Troubleshooting

- Ensure Docker is running and accessible.
- Verify that the specified port is not already in use.
- Check the script output for any error messages during the build or run process.
