# Lead Management System - Setup Guide

## Prerequisites
- **JDK 17** or higher
- **VS Code** with extensions:
  - Extension Pack for Java
  - Lombok Annotations Support for VS Code
- **Maven** (usually bundled with VS Code Java extensions)

## Project Structure (Hexagonal Architecture)

```
src/main/java/com/tekion/leadmanagement/
├── domain/                          # Pure business logic (no framework dependencies)
│   ├── lead/
│   │   ├── model/                   # Lead entities and value objects
│   │   └── port/                    # Interfaces for persistence
│   ├── notification/
│   │   ├── model/                   # Notification entities
│   │   └── port/                    # Interfaces for notification services
│   └── scoring/
│       ├── model/                   # Scoring entities
│       ├── rule/                    # Scoring rules
│       └── service/                 # Scoring engine
│
├── application/                     # Use case orchestration
│   ├── lead/                        # Lead service (orchestrates domain logic)
│   └── notification/                # Notification router
│
├── adapter/                         # External implementations
│   ├── persistence/
│   │   └── inmemory/                # In-memory repository implementation
│   └── notification/
│       ├── email/                   # Email notification adapter
│       └── sms/                     # SMS notification adapter
│
└── bootstrap/                       # Application entry point
    └── Main.java                    # Manual dependency injection & demo
```

## Hexagonal Architecture Layers

1. **Domain Layer** (`domain/`): Core business logic, entities, and port interfaces
2. **Application Layer** (`application/`): Use case orchestration
3. **Adapter Layer** (`adapter/`): Concrete implementations of ports
4. **Bootstrap Layer** (`bootstrap/`): Wiring and application startup

## Running the Application

### Option 1: VS Code
1. Open `src/main/java/com/tekion/leadmanagement/bootstrap/Main.java`
2. Click **Run** above the `main` method

### Option 2: Maven Command Line
```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package
mvn package

# Run the application
mvn exec:java -Dexec.mainClass="com.tekion.leadmanagement.bootstrap.Main"
```

## Lombok Configuration

Lombok is configured in `pom.xml` with:
- Dependency scope: `provided`
- Annotation processor path configured in maven-compiler-plugin

VS Code should automatically detect Lombok through the extension. If you see errors:
1. Ensure "Lombok Annotations Support for VS Code" extension is installed
2. Reload VS Code window: `Ctrl+Shift+P` → "Developer: Reload Window"
3. Check Java Language Server settings for annotation processing

## Next Steps

1. Create domain models in `domain/lead/model/`
2. Define port interfaces in `domain/lead/port/`
3. Implement scoring rules in `domain/scoring/rule/`
4. Build application services in `application/`
5. Create adapters in `adapter/`
6. Wire everything in `bootstrap/Main.java`

