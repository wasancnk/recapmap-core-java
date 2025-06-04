# Real-time Communication Strategy

**Date**: June 1, 2025  
**Project**: RecapMap Java Backend  
**Focus**: Frontend ↔ Backend Real-time Sync

## Communication Options Comparison

### 1. WebSockets
```yaml
description: "Full-duplex communication channel"
best_for:
  - "Real-time mindmap collaboration"
  - "Live AI chat streaming"
  - "Instant validation feedback"

pros:
  - "True bidirectional communication"
  - "Low latency for frequent updates"
  - "Perfect for collaborative editing"
  - "Spring WebSocket has good support"

cons:
  - "More complex to implement"
  - "Requires connection state management"
  - "Harder to scale horizontally"

use_cases:
  - "Multiple users editing same mindmap"
  - "Live cursor positions and selections"
  - "Real-time AI responses streaming"
```

### 2. Server-Sent Events (SSE)
```yaml
description: "Server pushes data to client over HTTP"
best_for:
  - "One-way data streaming"
  - "Progress updates"
  - "Notifications"

pros:
  - "Simpler than WebSockets"
  - "Built-in reconnection"
  - "Works through proxies/firewalls"
  - "HTTP-based (easier debugging)"

cons:
  - "One-way only (server → client)"
  - "Still need regular HTTP for client → server"
  - "Limited browser connection limits"

use_cases:
  - "YAML export progress updates"
  - "AI processing status"
  - "System notifications"
```

### 3. Long Polling
```yaml
description: "Client keeps HTTP connection open for updates"
best_for:
  - "Simple real-time needs"
  - "Low-frequency updates"

pros:
  - "Works with any HTTP infrastructure"
  - "Simple to implement and debug"
  - "No special browser requirements"

cons:
  - "Higher latency than WebSockets"
  - "More server resources (holding connections)"
  - "Not great for high-frequency updates"

use_cases:
  - "Project status changes"
  - "Infrequent collaboration updates"
```

## Recommended Strategy for RecapMap

### Phase 1: WebSockets for Core Features
```yaml
rationale: "RecapMap needs true real-time collaboration"

implementation:
  primary: "WebSocket for mindmap editing"
  fallback: "REST API for basic operations"
  
features_needing_realtime:
  - "Multiple users editing same mindmap"
  - "Live node dragging and connections"
  - "Real-time AI chat responses"
  - "Instant validation as user types"
  - "Cursor positions and selections"
```

### Phase 2: Hybrid Approach
```yaml
websocket:
  - "Collaborative editing"
  - "AI streaming responses"
  - "Live validation"

sse:
  - "Export progress"
  - "Background AI processing"
  - "System notifications"

rest_api:
  - "Project CRUD operations"
  - "User authentication"
  - "File uploads/downloads"
```

## WebSocket Implementation Plan

### 1. Spring WebSocket Setup
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new MindMapWebSocketHandler(), "/ws/mindmap")
                .setAllowedOrigins("*") // Configure properly for production
                .withSockJS(); // Fallback for older browsers
    }
}
```

### 2. Message Types
```yaml
mindmap_collaboration:
  - "NODE_UPDATED"
  - "NODE_MOVED" 
  - "CONNECTION_CREATED"
  - "CONNECTION_DELETED"
  - "USER_CURSOR_MOVED"
  - "USER_SELECTION_CHANGED"

ai_interaction:
  - "AI_MESSAGE_STREAMING"
  - "AI_PROCESSING_STATUS"
  - "AI_RESPONSE_COMPLETE"

project_management:
  - "PROJECT_SAVED"
  - "VALIDATION_RESULT"
  - "EXPORT_PROGRESS"
```

### 3. Message Structure
```json
{
  "type": "NODE_UPDATED",
  "projectId": "project-123",
  "userId": "user-456", 
  "timestamp": "2025-06-01T10:30:00Z",
  "data": {
    "nodeId": "node-789",
    "changes": {
      "title": "New Title",
      "position": { "x": 100, "y": 200 }
    }
  }
}
```

## Authentication for WebSockets

### Token-Based Auth
```yaml
approach: "JWT token validation on WebSocket connection"

flow:
  1. "Frontend gets JWT from REST login"
  2. "WebSocket connection includes token in headers"
  3. "Backend validates token and stores user session"
  4. "All WebSocket messages include user context"

implementation:
  - "Spring Security WebSocket support"
  - "Custom handshake interceptor for token validation"
  - "Session management for active connections"
```

## Scaling Considerations

### Single Instance (Phase 1)
```yaml
approach: "In-memory session management"
limitations: "Single server, no horizontal scaling"
suitable_for: "MVP and early users"
```

### Multi-Instance (Phase 2)
```yaml
approach: "Redis for shared WebSocket state"
benefits: "Multiple backend instances, load balancing"
implementation: "Spring Session + Redis WebSocket support"
```

### Microservices (Phase 3)
```yaml
approach: "Dedicated WebSocket gateway service"
benefits: "Scalable real-time communication"
tools: "Spring Cloud Gateway + Message Broker"
```

## Development Priority

### Must Have (Phase 1)
1. **Basic WebSocket connection** for mindmap sync
2. **Node update broadcasting** for collaboration
3. **Simple authentication** via JWT

### Nice to Have (Phase 2)
1. **Live cursor tracking** for better UX
2. **AI response streaming** for better perceived performance
3. **Conflict resolution** for simultaneous edits

### Future (Phase 3)
1. **Advanced collaboration features** (operational transforms)
2. **Performance optimizations** (delta updates only)
3. **Offline support** with sync on reconnection

This gives you a clear path from simple WebSocket implementation to advanced real-time collaboration features.
