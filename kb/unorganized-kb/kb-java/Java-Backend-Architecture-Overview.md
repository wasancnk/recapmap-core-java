# Java Backend Architecture Overview

**Date**: June 1, 2025  
**Project**: RecapMap Java Backend  
**Phase**: Monolith-First Strategy

## Architecture Philosophy

### Monolith-First Approach
```yaml
strategy: "Start simple, scale smart"
rationale:
  - "Faster initial development and testing"
  - "Easier debugging and monitoring"
  - "Clear refactoring path to microservices"
  - "Reduced infrastructure complexity"

evolution_path:
  phase_1: "Spring Boot Monolith"
  phase_2: "Modular Monolith (prepare for split)"
  phase_3: "Strategic Microservices (high-load components)"
```

### Core Responsibilities
1. **Security Boundary** - Zero trust frontend, all auth/validation in Java
2. **YAML Generation** - Convert visual mindmaps to AI-ready specifications
3. **AI Orchestration** - Manage AI agent conversations and memory
4. **Business Logic** - Execute generated business rules
5. **Real-time Sync** - Keep frontend mindmaps synchronized

## Service Structure (Monolith Layout)

### Layer Architecture
```
┌─────────────────────────────────────┐
│           REST Controllers          │ ← Frontend API
├─────────────────────────────────────┤
│           Service Layer             │ ← Business Logic
├─────────────────────────────────────┤
│           Domain Models             │ ← Core Entities
├─────────────────────────────────────┤
│         Repository Layer            │ ← Data Access
├─────────────────────────────────────┤
│        External Integrations       │ ← AI APIs, etc.
└─────────────────────────────────────┘
```

### Package Structure
```
com.recapmap.backend/
├── config/           # Spring configuration
├── controller/       # REST endpoints
├── service/          # Business logic
├── domain/          # Core entities
├── repository/      # Data access
├── integration/     # External APIs (OpenAI, etc.)
├── security/        # Auth & authorization
├── websocket/       # Real-time communication
└── utils/           # Shared utilities
```

## Core Services

### 1. MindMapService
- **Purpose**: CRUD operations for mindmaps
- **Key Methods**: create, update, delete, export
- **Dependencies**: MindMapRepository, YamlExportService

### 2. YamlExportService  
- **Purpose**: Convert mindmaps to AI-ready YAML
- **Key Methods**: generateYaml, validateExport
- **Dependencies**: MindMapService (reuse your existing logic)

### 3. AIOrchestrationService
- **Purpose**: Manage AI conversations and memory
- **Key Methods**: sendPrompt, manageContext, trackConversation
- **Dependencies**: OpenAI/Claude APIs

### 4. ProjectService
- **Purpose**: Handle project lifecycle and collaboration
- **Key Methods**: createProject, shareProject, manageVersions
- **Dependencies**: ProjectRepository, SecurityService

### 5. SecurityService
- **Purpose**: Authentication and authorization
- **Key Methods**: authenticate, authorize, validateToken
- **Dependencies**: JWT library, user repository

## Technology Stack

### Core Framework
```yaml
spring_boot: "3.2.x"
java_version: "17+"
build_tool: "Maven"
database: "MongoDB" # (matching your existing choice)

dependencies:
  security: "Spring Security 6"
  web: "Spring WebMVC + WebSocket"
  data: "Spring Data MongoDB"
  validation: "Bean Validation"
  testing: "Spring Boot Test + Testcontainers"
```

### External Integrations
```yaml
ai_providers:
  openai: "Direct API calls"
  claude: "Direct API calls"
  
monitoring:
  logging: "Logback + structured JSON"
  metrics: "Micrometer (future: Prometheus)"
  
real_time:
  websocket: "Spring WebSocket"
  message_broker: "Simple in-memory (phase 1)"
```

## Communication Patterns

### Frontend ↔ Backend
```yaml
rest_api:
  - "Standard CRUD operations"
  - "YAML export endpoints"
  - "Project management"

websocket:
  - "Real-time mindmap collaboration"
  - "AI conversation streaming"
  - "Live validation feedback"

authentication:
  - "JWT tokens in HTTP headers"
  - "WebSocket auth via token validation"
```

### Backend ↔ AI Services
```yaml
pattern: "Direct HTTP calls with custom orchestration"
benefits:
  - "Full control over conversation flow"
  - "Custom memory management"
  - "Cost optimization and monitoring"
  - "Multiple AI provider support"
```

## Scalability Considerations

### Immediate (Monolith)
- **Database**: MongoDB with proper indexing
- **Caching**: Spring Cache with in-memory storage
- **Connection Pooling**: HikariCP for any SQL needs
- **WebSocket**: Session-based state management

### Future (Microservice Ready)
- **Database**: Split by domain (User data, Projects, AI Context)
- **Message Queue**: RabbitMQ or Kafka for async processing
- **Cache**: Redis for shared state
- **API Gateway**: Spring Cloud Gateway

## Development Workflow

### Local Development
```yaml
setup:
  - "MongoDB running locally"
  - "Spring Boot DevTools for hot reload"
  - "Frontend proxy to backend (Vite → Spring Boot)"

testing:
  - "Unit tests with Spring Boot Test"
  - "Integration tests with Testcontainers"
  - "API testing with REST Assured"
```

### Deployment Strategy
```yaml
phase_1: "Single JAR deployment"
phase_2: "Docker containerization" 
phase_3: "Kubernetes for microservices"
```

## Next Steps

1. **Set up basic Spring Boot project structure**
2. **Implement core domain models** (reuse your MindMap entities)
3. **Create REST controllers** for CRUD operations
4. **Integrate YAML export service** (port existing logic)
5. **Add AI orchestration layer**
6. **Implement WebSocket for real-time features**

This monolith approach keeps complexity low while building a solid foundation for future scaling.
