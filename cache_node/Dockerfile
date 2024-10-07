# Use a base image with Java
FROM openjdk:21-jdk-slim

# Set a default environment variable for the port
ENV PORT=50051

# Expose the port specified by the PORT environment variable
EXPOSE $PORT

# Argument for the JAR file; default to app.jar if not specified
ARG JAR_FILE=app.jar

# Add the JAR file to the container
ADD ${JAR_FILE} app.jar

# Command to run the application, using the PORT environment variable
CMD ["sh", "-c", "java -jar app.jar $PORT"]