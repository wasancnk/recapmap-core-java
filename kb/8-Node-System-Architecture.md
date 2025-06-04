# 8-Node System Architecture: Universal Business Logic Modeling

**Date**: May 31, 2025  
**Project**: RecapMap Core React Frontend  
**Purpose**: Complete specification of the 8-node system for modeling any business logic

## Architecture Overview

The 8-node system provides a **complete vocabulary** for describing any business application through visual modeling. This universal language ensures that complex business logic can be captured visually and translated perfectly into AI task descriptions.

## The Complete 8-Node System

### 1. Base Node - Abstract Foundation
**Type**: Abstract base class (cannot be instantiated directly)
**Purpose**: Provides common structure and behavior for all other nodes
**Role**: Foundation layer that ensures consistency across all node types

#### Shared Properties:
- `id`: Unique identifier for the node
- `title`: Human-readable name/label
- `description`: Detailed explanation for AI context (mandatory for all nodes)
- `position`: X,Y coordinates on canvas
- `metadata`: Creation date, version, author

#### Common Behaviors:
- Click handling for property panel opening
- Chat integration (click to mention in chat conversations)
- Drag and drop interactions
- Connection point management (input/output handles)
- Selection and multi-selection support
- Basic validation states (complete, incomplete, error)

#### Visual Standards:
- Consistent sizing across all node types
- Standardized connection handles (top input, bottom output)
- Uniform typography and spacing
- Shared animation and interaction patterns

---

### 2. Use Case Node - Feature/Intent Anchor
**Color**: Blue | **Icon**: Target/Bullseye
**Purpose**: Top-level business feature or user story definition
**Role**: Triggers complete YAML module generation for entire features

#### Specific Properties:
- `priority`: 'low' | 'medium' | 'high' | 'critical'
- `acceptance_criteria`: List of success conditions
- `business_value`: ROI or impact description
- `stakeholders`: List of involved parties
- `epic_reference`: Connection to larger business initiatives
- `success_metrics`: Measurable outcomes
- `estimated_effort`: Development complexity assessment

#### Business Logic Context:
- Serves as the root node for feature aggregation
- All related nodes (Screen, User, Process, etc.) link to Use Case
- Triggers comprehensive YAML export including all connected nodes
- Provides business context for AI code generation

#### YAML Generation Role:
```yaml
feature_name: "user_authentication"
use_case_priority: "high"
business_value: "Secure user access foundation"
acceptance_criteria:
  - "Users can register with email/password"
  - "Secure login with session management"
  - "Password reset functionality"
```

---

### 3. Screen Node - UI Representation
**Color**: Green | **Icon**: Monitor/Display
**Purpose**: Visual interface components and user experience design
**Role**: Maps to React components, UI layouts, or frontend specifications

#### Specific Properties:
- `screenType`: 'dashboard' | 'form' | 'list' | 'detail' | 'modal' | 'wizard' | 'landing'
- `layoutType`: 'grid' | 'flex' | 'fixed' | 'responsive'
- `components`: List of UI elements (buttons, inputs, tables, etc.)
- `responsive`: Boolean for mobile/tablet behavior
- `accessibility`: Boolean for WCAG compliance
- `navigation_flow`: How users move between screens
- `data_bindings`: What data sources feed this screen
- `validation_rules`: Frontend validation requirements

#### Business Logic Context:
- Represents what users see and interact with
- Connects to User nodes (who can access this screen)
- Links to Process/Tool nodes (what actions are available)
- Associates with Storage nodes (what data is displayed)

#### Architectural Clarity:
- **No Framework Selection**: Screen nodes describe WHAT interface is needed, not HOW to implement it
- **Platform Abstraction**: RecapMap's React engine handles all technical implementation
- **Business Focus**: Users specify functionality and layout, not technology choices

#### YAML Generation Role:
```yaml
screen_name: "login_form"
screen_type: "form"
layout_type: "flex"
responsive: true
accessibility: true
components:
  - type: "email_input"
    validation: "required|email"
  - type: "password_input" 
    validation: "required|min:8"
  - type: "submit_button"
    text: "Login"
navigation_flow:
  - on_success: "dashboard"
  - on_error: "error_screen"
```

**Note**: Framework is not specified - RecapMap's React engine handles implementation details.

---

### 4. User Node - Actor/Persona
**Color**: Orange | **Icon**: Person/User
**Purpose**: Define who interacts with the system and their capabilities
**Role**: Establishes permissions, roles, and system entry points

#### Specific Properties:
- `role`: 'admin' | 'user' | 'guest' | 'manager' | 'analyst' (custom roles)
- `permissions`: List of allowed actions and access levels
- `access_level`: 'read' | 'write' | 'admin' | 'super_admin'
- `authentication_method`: 'password' | 'oauth' | 'sso' | 'api_key'
- `persona_details`: Demographics, goals, pain points
- `workflow_entry_points`: Which screens/processes they initiate
- `business_context`: Department, seniority, responsibilities

#### Business Logic Context:
- Defines security boundaries and access control
- Connects to Screen nodes (which interfaces they can access)
- Links to Process/Tool nodes (which capabilities they can use)
- Associates with Use Case nodes (which features they benefit from)

#### YAML Generation Role:
```yaml
user_role: "admin"
permissions:
  - "user_management"
  - "system_configuration"
  - "data_export"
access_level: "admin"
authentication: "password_with_mfa"
```

---

### 5. Process/Tool Node - Function/Service/Capability
**Color**: Purple | **Icon**: Gear/Cog
**Purpose**: Business logic operations and system capabilities
**Role**: Maps to Java capabilities, service modules, or API functions

#### Specific Properties:
- `capability_name`: Exact Java function name for backend mapping
- `input_parameters`: Required and optional inputs with types
- `output_parameters`: Expected outputs and return types
- `service_type`: 'computation' | 'integration' | 'notification' | 'validation'
- `external_dependencies`: APIs, databases, or services required
- `error_conditions`: Expected failure modes and handling
- `performance_requirements`: Response time, throughput expectations

#### Business Logic Context:
- Represents the "what happens" in business workflows
- Connects to User nodes (who can execute this process)
- Links to Storage nodes (what data it reads/writes)
- Associates with Flow Controller nodes (when it gets triggered)
- Can be triggered by Screen interactions or other processes

#### YAML Generation Role:
```yaml
capability_name: "authenticate_user"
input_parameters:
  email: "string|required"
  password: "string|required"
output_parameters:
  user_session: "object"
  authentication_token: "string"
service_type: "validation"
```

---

### 6. Storage Node - Data Layer
**Color**: Yellow | **Icon**: Database/Cylinder
**Purpose**: Data persistence, caching, and state management
**Role**: Triggers schema YAML for MongoDB, AuraDB, or filesystem storage

#### Specific Properties:
- `storage_type`: 'database' | 'cache' | 'file_system' | 'memory' | 'external_api'
- `data_schema`: Structure definition with field types and constraints
- `indexing_strategy`: Performance optimization specifications
- `backup_requirements`: Data retention and recovery policies
- `access_patterns`: How data is typically read/written
- `scaling_considerations`: Growth expectations and partitioning
- `security_classification`: 'public' | 'internal' | 'confidential' | 'restricted'

#### Business Logic Context:
- Represents where information lives in the system
- Connects to Process/Tool nodes (what operations read/write this data)
- Links to Screen nodes (what data is displayed to users)
- Associates with User nodes (data access permissions)

#### YAML Generation Role:
```yaml
storage_name: "user_profiles"
storage_type: "database"
schema:
  user_id: "uuid|primary_key"
  email: "string|unique|indexed"
  profile_data: "json"
indexing: ["email", "created_date"]
```

---

### 7. Flow Controller Node - Decision Logic/Routing
**Color**: Red | **Icon**: Diamond/Switch
**Purpose**: Business rules, conditional logic, and workflow routing
**Role**: Maps to if/else statements, routing logic, parallel processing, and decision trees

#### Specific Properties:
- `controller_type`: 'decision' | 'router' | 'parallel' | 'merge' | 'loop' | 'timer'
- `conditions`: IF-THEN logic rules with boolean expressions
- `routing_rules`: Where flow continues based on conditions
- `parallel_branches`: Concurrent execution paths
- `merge_strategy`: How parallel branches recombine
- `timeout_handling`: Time-based decision making
- `default_path`: Fallback behavior when conditions aren't met

#### Business Logic Context:
- Controls the flow of business processes
- Connects between other nodes to create workflow logic
- Links to Process/Tool nodes (what gets executed based on decisions)
- Can trigger different Screen nodes based on conditions
- Manages complex business rule implementation

#### YAML Generation Role:
```yaml
controller_type: "decision"
conditions:
  - if: "user.role == 'admin'"
    then: "route_to_admin_dashboard"
  - if: "user.is_first_login == true"
    then: "route_to_onboarding"
default_path: "route_to_user_dashboard"
```

---

### 8. Error/Rejection Node - Failsafe/Fallback
**Color**: Gray | **Icon**: Warning/Alert
**Purpose**: Error handling, edge cases, and system resilience
**Role**: Maps to try/catch blocks, alerts, retry policies, and graceful degradation

#### Specific Properties:
- `error_type`: 'validation' | 'system' | 'network' | 'business_rule' | 'security'
- `detection_conditions`: When this error scenario is triggered
- `fallback_action`: What happens when error occurs
- `retry_strategy`: Automatic retry attempts and backoff
- `user_notification`: How errors are communicated to users
- `logging_requirements`: What information gets captured
- `escalation_rules`: When to alert administrators or support

#### Business Logic Context:
- Ensures system robustness and user experience quality
- Connects to any node that might fail or produce errors
- Links to User nodes (how different roles see errors)
- Associates with Process/Tool nodes (what recovery actions are available)

#### YAML Generation Role:
```yaml
error_type: "validation"
detection_condition: "invalid_email_format"
fallback_action: "show_validation_message"
user_notification: "Please enter a valid email address"
retry_strategy: "none"
```

## Node Relationship Patterns

### Connection Rules
- **Use Case nodes** can connect to any other node type
- **Screen nodes** typically connect to User and Process/Tool nodes
- **User nodes** connect to Screen and Process/Tool nodes they can access
- **Process/Tool nodes** connect to Storage nodes and other Process/Tool nodes
- **Storage nodes** can connect to multiple Process/Tool nodes
- **Flow Controller nodes** act as junction points between any node types
- **Error/Rejection nodes** can connect from any node that might fail

### Semantic Relationships
- **Triggers**: Use Case → Screen → Process/Tool → Storage
- **Permissions**: User → Screen → Process/Tool → Storage
- **Dependencies**: Process/Tool ↔ Storage ↔ Flow Controller
- **Error Handling**: Any Node → Error/Rejection → Recovery Path

## YAML Export Strategy

### Individual Node YAML
Each node generates its own semantic YAML section with:
- Node type and properties
- Connections to other nodes
- Business context and reasoning
- AI generation instructions

### Use Case Aggregation
All nodes connected to a Use Case are merged into a comprehensive YAML:
- Complete feature specification
- All supporting components included
- Rich context for AI code generation
- Cross-node relationships preserved

### Graph Traversal Context
When AI needs broader context:
- Traverse connected nodes for additional information
- Score relevance of adjacent nodes
- Include high-scoring nodes in task description
- Provide comprehensive business understanding

## Design Principles

### Completeness
The 8 nodes can model any business application:
- User interactions (User + Screen)
- Business logic (Process/Tool + Flow Controller)
- Data management (Storage)
- Error handling (Error/Rejection)
- Feature organization (Use Case)

### Simplicity
Despite covering complex business logic:
- Only 8 node types to learn
- Clear semantic meaning for each
- Consistent interaction patterns
- Visual differentiation through color/icon

### AI Optimization
Every aspect designed for perfect AI task descriptions:
- Rich semantic properties
- Clear business context
- Unambiguous relationships
- Complete implementation details

This 8-node architecture provides the foundation for transforming any business vision into a deployable application through visual design and AI-assisted code generation.

---

**Next Phase**: Implement this architecture in React Flow with TypeScript type definitions and Zustand state management.
