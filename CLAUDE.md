# USRV Project Guide

## Build Commands
- Build: `./gradlew build`
- Run: `./gradlew run`
- Test all: `./gradlew test`
- Run single test: `./gradlew test --tests "TestClassName.testMethodName"`
- Clean: `./gradlew clean`

## Code Style
- **Naming**: Classes=PascalCase, methods/variables=camelCase, constants=UPPER_SNAKE_CASE
- **Indentation**: 4 spaces
- **Braces**: K&R style (opening brace on same line)
- **Imports**: Java standard first, then third-party, then project-specific
- **Error handling**: Use custom exceptions from exceptions package; chain causes
- **Logging**: Use org.usrv.util.Logger class for consistent logging
- **Immutability**: Prefer immutable objects where possible
- **Documentation**: Self-documenting code with clear method/variable names

## Architecture
- Package by feature/domain
- Clear separation of concerns (server, config, file handling)
- Custom exceptions for different error scenarios