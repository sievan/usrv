# USRV - Micro HTTP Static File Server

A lightweight, high-performance HTTP server for serving static files written in Java. USRV is designed to efficiently serve web assets with configurable settings for different deployment scenarios.

## Features

- Fast static file serving with in-memory caching
- Support for Single Page Applications (SPA) mode
- Automatic MIME type detection
- Virtual thread per request for high concurrency
- Request logging with unique request IDs
- Graceful error handling with custom error pages
- Configurable port and directory settings

## Requirements

- Java 21 or higher
- Gradle build system

## Building and Running

### Build Commands

```bash
# Build the project
./gradlew build

# Run the server
./gradlew run

# Run all tests
./gradlew test

# Run a specific test
./gradlew test --tests "TestClassName.testMethodName"

# Clean build artifacts
./gradlew clean
```

## Usage

The server starts by default on port 80 and serves files from the `./dist` directory:

```java
// Basic usage with default configuration
Server server = new Server();
server.start();

// Custom configuration
ServerConfig config = new ServerConfig("./public", 8080, true);
Server server = new Server(config);
server.start();
```

## Configuration Options

The server can be configured with the following options:

- `distFolder` - Directory containing static files to serve (default: `./dist`)
- `port` - Port to listen on (default: `80`)
- `serveSingleIndex` - SPA mode, serving index.html for all HTML requests (default: `false`)

```java
// Create custom configuration
ServerConfig config = new ServerConfig(
    "./public",  // Static files directory
    8080,        // Port to listen on
    true         // Enable SPA mode
);
```

## Architecture

### Core Components

- **Server** - HTTP server implementation with request handling and caching
- **ClientRequest** - Parses and validates HTTP requests
- **Response** - Builds HTTP responses with appropriate headers
- **StaticFile** - Handles file loading and MIME type detection
- **ServerConfig** - Configuration options for the server

### Error Handling

Custom exceptions are used for different error scenarios:
- `InvalidRequestException` - For malformed requests
- `RequestParsingException` - When request parsing fails
- `UnsupportedMethodException` - For unsupported HTTP methods

### Logging

The application uses SLF4J with Logback for logging, with request IDs for traceability.

## Code Style

- **Naming**: Classes=PascalCase, methods/variables=camelCase, constants=UPPER_SNAKE_CASE
- **Indentation**: 4 spaces
- **Braces**: K&R style (opening brace on same line)
- **Error handling**: Custom exceptions with cause chaining
- **Immutability**: Preference for immutable objects

## License

This project is proprietary and confidential.