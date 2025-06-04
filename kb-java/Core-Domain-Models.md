# Core Domain Models for Java Backend

**Date**: June 1, 2025  
**Project**: RecapMap Java Backend  
**Focus**: Domain-Driven Design Entities

## Domain Architecture Overview

### Domain Boundaries
```yaml
core_domains:
  project_management: "Projects, workspaces, collaboration"
  mindmap_design: "Nodes, connections, visual layout"
  ai_orchestration: "Conversations, context, memory"
  export_generation: "YAML, code generation, artifacts"
  user_management: "Authentication, authorization, preferences"
```

## 1. Project Domain

### Project Entity
```java
@Document(collection = "projects")
public class Project {
    @Id
    private String id;
    
    private String name;
    private String description;
    private String version;
    
    private String ownerId;
    private List<String> collaboratorIds;
    private ProjectStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String author;
    private List<String> tags;
    
    private boolean isModified;
    private boolean autoSave;
    
    // Collaboration
    private CollaborationSettings collaborationSettings;
    private List<ProjectActivity> recentActivity;
    
    // Export history
    private List<ExportRecord> exportHistory;
    private LocalDateTime lastExportedAt;
    
    public enum ProjectStatus {
        DRAFT, ACTIVE, ARCHIVED, SHARED, EXPORTED
    }
    
    @Embeddable
    public static class CollaborationSettings {
        private boolean allowEdit;
        private boolean allowComment;
        private boolean allowExport;
        private ShareVisibility visibility;
        
        public enum ShareVisibility {
            PRIVATE, TEAM, ORGANIZATION, PUBLIC
        }
    }
    
    @Embeddable
    public static class ProjectActivity {
        private String userId;
        private String action; // "node_created", "connection_added", etc.
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;
    }
    
    @Embeddable
    public static class ExportRecord {
        private String exportId;
        private String format; // "yaml", "json", etc.
        private LocalDateTime exportedAt;
        private String exportedBy;
        private long fileSize;
        private String checksum;
    }
}
```

### ProjectRepository
```java
@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {
    List<Project> findByOwnerIdOrderByUpdatedAtDesc(String ownerId);
    List<Project> findByCollaboratorIdsContaining(String userId);
    List<Project> findByStatusAndCollaborationSettings_Visibility(
        Project.ProjectStatus status, 
        Project.CollaborationSettings.ShareVisibility visibility
    );
    
    @Query("{ 'tags': { $in: ?0 } }")
    List<Project> findByTagsIn(List<String> tags);
}
```

## 2. MindMap Domain

### MindMapNode Entity (Enhanced from your existing)
```java
@Document(collection = "mindmap_nodes")
public class MindMapNode {
    @Id
    private String id;
    
    private String projectId;
    private String title;
    private String description;
    
    private NodeType type;
    private NodeStatus status;
    
    private Position position;
    private Dimensions dimensions;
    private Styling styling;
    
    private NodeConnections connections;
    private NodeProperties properties;
    private BusinessLogic businessLogic;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String lastModifiedBy;
    
    // AI Integration
    private AIContext aiContext;
    
    public enum NodeType {
        USECASE, ACTOR, PROCESS, DATA, SYSTEM, CONDITION, DECISION, SCREEN
    }
    
    public enum NodeStatus {
        DRAFT, VALIDATED, EXPORTED, ARCHIVED
    }
    
    @Embeddable
    public static class Position {
        private double x;
        private double y;
        private int zIndex;
    }
    
    @Embeddable
    public static class Dimensions {
        private double width;
        private double height;
        private boolean autoResize;
    }
    
    @Embeddable
    public static class Styling {
        private String backgroundColor;
        private String borderColor;
        private String textColor;
        private String fontSize;
        private boolean isSelected;
        private boolean isHighlighted;
    }
    
    @Embeddable
    public static class NodeConnections {
        private List<String> inputs;
        private List<String> outputs;
        private List<String> bidirectional;
    }
    
    @Embeddable
    public static class NodeProperties {
        // Dynamic properties based on node type
        private Map<String, Object> properties;
        
        // Type-specific validation rules
        private Map<String, String> validationRules;
        
        // Properties for different node types
        public void setUseCaseProperties(List<String> acceptanceCriteria, String priority) {
            properties.put("acceptanceCriteria", acceptanceCriteria);
            properties.put("priority", priority);
        }
        
        public void setProcessProperties(List<String> inputs, List<String> outputs, String method) {
            properties.put("inputs", inputs);
            properties.put("outputs", outputs);
            properties.put("method", method);
        }
    }
    
    @Embeddable
    public static class BusinessLogic {
        private List<String> inputParameters;
        private List<String> outputParameters;
        private Map<String, String> validationRules;
        private List<BusinessRule> businessRules;
        
        @Embeddable
        public static class BusinessRule {
            private String condition;
            private String action;
            private String description;
            private RulePriority priority;
            
            public enum RulePriority {
                LOW, MEDIUM, HIGH, CRITICAL
            }
        }
    }
    
    @Embeddable
    public static class AIContext {
        private String aiGeneratedDescription;
        private List<String> aiSuggestions;
        private String lastAIInteraction;
        private LocalDateTime lastAIUpdate;
        private Map<String, Object> aiMetadata;
    }
}
```

### MindMapConnection Entity
```java
@Document(collection = "mindmap_connections")
public class MindMapConnection {
    @Id
    private String id;
    
    private String projectId;
    private String sourceNodeId;
    private String targetNodeId;
    
    private ConnectionType type;
    private String label;
    private String description;
    
    private ConnectionStyling styling;
    private ConnectionProperties properties;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    
    public enum ConnectionType {
        DEPENDS_ON("depends_on"),
        TRIGGERS("triggers"),
        PROVIDES("provides"),
        SEQUENCE("sequence"),
        IF_THEN("if_then"),
        CONTAINS("contains"),
        INHERITS("inherits"),
        IMPLEMENTS("implements");
        
        private final String value;
        
        ConnectionType(String value) {
            this.value = value;
        }
    }
    
    @Embeddable
    public static class ConnectionStyling {
        private String color;
        private Integer thickness;
        private String dashPattern; // "solid", "dashed", "dotted"
        private ArrowType sourceArrow;
        private ArrowType targetArrow;
        
        public enum ArrowType {
            NONE, ARROW, CIRCLE, SQUARE, DIAMOND
        }
    }
    
    @Embeddable
    public static class ConnectionProperties {
        private Map<String, Object> metadata;
        private List<String> conditions;
        private String dataFlow;
        private Integer weight; // For priority/importance
    }
}
```

## 3. AI Integration Domain

### AIConversation Entity
```java
@Document(collection = "ai_conversations")
public class AIConversation {
    @Id
    private String id;
    
    private String projectId;
    private String userId;
    private String contextType; // "project", "node", "export", "general"
    private String contextId; // Related entity ID
    
    private List<AIMessage> messages;
    private AIConversationStatus status;
    
    private LocalDateTime startedAt;
    private LocalDateTime lastMessageAt;
    private LocalDateTime endedAt;
    
    // Memory and context
    private AIMemoryContext memoryContext;
    private Map<String, Object> conversationMetadata;
    
    public enum AIConversationStatus {
        ACTIVE, PAUSED, COMPLETED, ARCHIVED
    }
    
    @Embeddable
    public static class AIMessage {
        private String messageId;
        private MessageRole role;
        private String content;
        private LocalDateTime timestamp;
        private MessageStatus status;
        
        // AI-specific metadata
        private String model; // "gpt-4", "claude-3", etc.
        private Integer tokenCount;
        private Double confidence;
        private List<String> citations;
        
        public enum MessageRole {
            USER, AI_ASSISTANT, SYSTEM
        }
        
        public enum MessageStatus {
            PENDING, COMPLETED, FAILED, STREAMING
        }
    }
    
    @Embeddable
    public static class AIMemoryContext {
        private List<String> keyTopics;
        private Map<String, String> entityMentions;
        private List<String> previousDecisions;
        private String conversationSummary;
        private List<String> unresolvedQuestions;
    }
}
```

### AIProvider Entity
```java
@Document(collection = "ai_providers")
public class AIProvider {
    @Id
    private String id;
    
    private String name; // "OpenAI", "Anthropic", etc.
    private String baseUrl;
    private AIProviderType type;
    private AIProviderStatus status;
    
    private ProviderConfiguration configuration;
    private UsageMetrics usageMetrics;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    
    public enum AIProviderType {
        OPENAI, ANTHROPIC, CUSTOM
    }
    
    public enum AIProviderStatus {
        ACTIVE, INACTIVE, MAINTENANCE, ERROR
    }
    
    @Embeddable
    public static class ProviderConfiguration {
        private String apiKey;
        private String defaultModel;
        private Integer maxTokens;
        private Double temperature;
        private List<String> availableModels;
        private Map<String, Object> customSettings;
    }
    
    @Embeddable
    public static class UsageMetrics {
        private Long totalRequests;
        private Long totalTokens;
        private Double totalCost;
        private LocalDateTime lastReset;
        private Map<String, Object> monthlyUsage;
    }
}
```

## 4. Export Domain

### ExportJob Entity
```java
@Document(collection = "export_jobs")
public class ExportJob {
    @Id
    private String id;
    
    private String projectId;
    private String userId;
    private ExportType exportType;
    private ExportFormat format;
    
    private ExportJobStatus status;
    private ExportConfiguration configuration;
    
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    private String resultPath; // File path or URL
    private Long resultSize;
    private String resultChecksum;
    
    private ExportMetrics metrics;
    private List<ExportError> errors;
    
    public enum ExportType {
        YAML_EXPORT, CODE_GENERATION, DOCUMENTATION, VISUALIZATION
    }
    
    public enum ExportFormat {
        YAML, JSON, PDF, PNG, SVG, ZIP
    }
    
    public enum ExportJobStatus {
        QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED
    }
    
    @Embeddable
    public static class ExportConfiguration {
        private boolean includeMetadata;
        private boolean includePositions;
        private boolean includeAIContext;
        private String templateId;
        private Map<String, Object> customOptions;
    }
    
    @Embeddable
    public static class ExportMetrics {
        private Integer nodesProcessed;
        private Integer connectionsProcessed;
        private Long processingTimeMs;
        private Integer aiCallsMade;
        private Long totalTokensUsed;
    }
    
    @Embeddable
    public static class ExportError {
        private String errorCode;
        private String message;
        private String nodeId; // If error relates to specific node
        private String stackTrace;
        private LocalDateTime timestamp;
    }
}
```

## 5. User Domain

### User Entity
```java
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    
    private UserRole role;
    private UserStatus status;
    
    private UserPreferences preferences;
    private UserSubscription subscription;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime updatedAt;
    
    // Authentication
    private List<RefreshToken> refreshTokens;
    private LocalDateTime passwordChangedAt;
    
    public enum UserRole {
        ADMIN, USER, VIEWER, COLLABORATOR
    }
    
    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION
    }
    
    @Embeddable
    public static class UserPreferences {
        private String theme; // "light", "dark", "auto"
        private String language;
        private boolean emailNotifications;
        private boolean autoSave;
        private Map<String, Object> uiPreferences;
    }
    
    @Embeddable
    public static class UserSubscription {
        private SubscriptionTier tier;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private boolean autoRenew;
        
        public enum SubscriptionTier {
            FREE, BASIC, PROFESSIONAL, ENTERPRISE
        }
    }
    
    @Embeddable
    public static class RefreshToken {
        private String token;
        private LocalDateTime expiresAt;
        private boolean isRevoked;
        private LocalDateTime createdAt;
    }
}
```

## Domain Services

### 1. ProjectService
```java
@Service
@Transactional
public class ProjectService {
    
    public Project createProject(String name, String description, String ownerId) {
        // Implementation
    }
    
    public Project updateProject(String projectId, ProjectUpdateRequest request) {
        // Implementation with optimistic locking
    }
    
    public void shareProject(String projectId, List<String> collaboratorIds, ShareSettings settings) {
        // Implementation
    }
    
    public ValidationResult validateProject(String projectId) {
        // Integration with your existing validation logic
    }
}
```

### 2. MindMapService
```java
@Service
@Transactional
public class MindMapService {
    
    public MindMapNode createNode(String projectId, NodeCreationRequest request) {
        // Implementation
    }
    
    public MindMapConnection createConnection(String projectId, ConnectionCreationRequest request) {
        // Implementation with validation
    }
    
    public void updateNodePosition(String nodeId, Position newPosition) {
        // Real-time updates via WebSocket
    }
    
    public List<MindMapNode> getProjectNodes(String projectId) {
        // Implementation
    }
}
```

This domain model structure provides a solid foundation that aligns with your existing frontend types while adding the backend-specific features you'll need for persistence, collaboration, and AI integration.
