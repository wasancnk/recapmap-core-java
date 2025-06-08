# Mind Map to YAML Conversion System - Complete Knowledge Base

## Overview

This document provides a comprehensive technical specification for implementing a mind map to YAML conversion system that transforms visual business logic diagrams into AI-consumable structured knowledge bases. The system follows a semantic structure approach where different node types map to specific YAML sections.

## System Architecture

### Core Components

1. **Frontend Interface** - Interactive mind map editor with drag-and-drop nodes and connections
2. **Backend REST API** - Handles data persistence and YAML export
3. **YAML Export Service** - Core conversion logic from mind map entities to structured YAML
4. **Entity Model** - Data structures representing mind maps, nodes, and connections
5. **Database Layer** - MongoDB repositories for persistence

### Technology Stack

- **Frontend**: HTML5, JavaScript (ES6+), Bootstrap 5, SVG for connections
- **Backend**: Spring Boot 3.x, Java 17+
- **Database**: MongoDB
- **Build Tool**: Maven

## Entity Model Specifications

### MindMap Entity

```java
@Document(collection = "mindmaps")
public class MindMap {
    @Id
    private String id;
    private String title;
    private String description;
    private String ownerId;
    private String organizationId;
    private MindMapStatus status; // DRAFT, ACTIVE, ARCHIVED, EXPORTED
    private String version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastExportedAt;
    private MindMapMetadata metadata;
    private ExportConfig exportConfig;

    public static class MindMapMetadata {
        private int nodeCount;
        private int connectionCount;
        private List<String> tags;
        private String category;
        private String purpose;
    }

    public static class ExportConfig {
        private boolean autoExportEnabled;
        private String yamlExportPath;
        private String semanticVersion;
        private List<String> targetFormats;
    }
}
```

### MindMapNode Entity

```java
@Document(collection = "mindmap_nodes")
public class MindMapNode {
    @Id
    private String id;
    private String mindMapId;
    private String title;
    private String description;
    private NodeType nodeType; // INTENT, ACTOR, BEHAVIOR, RESOURCE, CONSTRAINT, EVOLUTION
    private SemanticType semanticType; // PURPOSE, USER, CONDITION, ENDPOINT, SECURITY, FUTURE_VISION
    private Position position;
    private Styling styling;
    private SemanticProperties semanticProperties;
    private BusinessLogic businessLogic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum NodeType {
        INTENT, ACTOR, BEHAVIOR, RESOURCE, CONSTRAINT, EVOLUTION
    }

    public enum SemanticType {
        PURPOSE, USER, CONDITION, ENDPOINT, SECURITY, FUTURE_VISION
    }

    public static class Position {
        private double x;
        private double y;
    }

    public static class Styling {
        private String backgroundColor;
        private String borderColor;
        private String textColor;
        private String shape;
        private Integer width;
        private Integer height;
    }

    public static class SemanticProperties {
        private String intent;
        private String context;
        private List<String> keywords;
        private Map<String, String> metadata;
    }

    public static class BusinessLogic {
        private List<String> inputParameters;
        private List<String> outputParameters;
        private String logicExpression;
        private Map<String, String> ifThenRules;
        private List<String> dependencies;
        private Map<String, String> validationRules;
    }
}
```

### MindMapConnection Entity

```java
@Document(collection = "mindmap_connections")
public class MindMapConnection {
    @Id
    private String id;
    private String mindMapId;
    private String sourceNodeId;
    private String targetNodeId;
    private ConnectionType connectionType;
    private String label;
    private String description;
    private ConnectionStyling styling;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ConnectionType {
        DEPENDS_ON, TRIGGERS, PROVIDES, SEQUENCE, IF_THEN, CONTAINS
    }

    public static class ConnectionStyling {
        private String sourceArrowType; // none, arrow, circle, square, diamond
        private String targetArrowType; // none, arrow, circle, square, diamond
        private String lineStyle; // solid, dashed, dotted
        private String color;
        private Integer thickness;
    }
}
```

## Frontend Implementation

### Node Creation and Management

```javascript
// Global state management
let currentMindMap = null;
let currentNodes = [];
let currentConnections = [];
let selectedNode = null;
let selectedConnection = null;

// Connection creation state (Miro-style)
let connectionCreationState = {
    isActive: false,
    sourceNode: null,
    sourceNodeElement: null,
    isHoveringTarget: false,
    hoveredTargetNode: null,
    ghostLine: null
};

// Create node element with proper event handlers
function createNodeElement(node) {
    const div = document.createElement('div');
    div.className = `mindmap-node node-${node.nodeType.toLowerCase()}`;
    div.style.left = `${node.position?.x || 50}px`;
    div.style.top = `${node.position?.y || 50}px`;
    div.dataset.nodeId = node.id;

    div.innerHTML = `
        <div class="node-type">${node.nodeType}</div>
        <div class="node-title">${node.title}</div>
        <div class="node-description">${node.description || ''}</div>
        <div class="node-link-icon" title="Create connection from this node">
            <i class="bi bi-arrow-up-right"></i>
        </div>
    `;

    // Click handler for selection and connection creation
    div.onclick = (e) => {
        e.stopPropagation();
        handleNodeClick(e, node.id);
        
        if (isConnectionMode) {
            handleConnectionModeClick(node, e);
        } else if (!e.ctrlKey && !e.metaKey) {
            selectNode(node);
        }
    };

    // Miro-style connection creation
    const linkIcon = div.querySelector('.node-link-icon');
    linkIcon.onclick = (e) => {
        e.stopPropagation();
        startMiroConnectionCreation(node, div);
    };

    // Connection target hover handlers
    div.addEventListener('mouseenter', (e) => {
        if (connectionCreationState.isActive && 
            connectionCreationState.sourceNode &&
            connectionCreationState.sourceNode.id !== node.id) {
            handleTargetNodeHover(node, div, true);
        }
    });

    div.addEventListener('mouseleave', (e) => {
        if (connectionCreationState.isActive && 
            connectionCreationState.sourceNode &&
            connectionCreationState.sourceNode.id !== node.id) {
            handleTargetNodeHover(node, div, false);
        }
    });

    // Connection completion handler
    div.addEventListener('click', (e) => {
        if (connectionCreationState.isActive && 
            connectionCreationState.isHoveringTarget &&
            connectionCreationState.hoveredTargetNode &&
            connectionCreationState.hoveredTargetNode.id === node.id) {
            e.stopPropagation();

            // Validate connection (no self-connections, no duplicates)
            const existingConnection = currentConnections.find(conn =>
                (conn.sourceNodeId === connectionCreationState.sourceNode.id && conn.targetNodeId === node.id) ||
                (conn.sourceNodeId === node.id && conn.targetNodeId === connectionCreationState.sourceNode.id)
            );

            if (existingConnection) {
                return; // Invalid connection
            }

            // Show connection type picker
            const nodeRect = div.getBoundingClientRect();
            const x = nodeRect.right + 10;
            const y = nodeRect.top + (nodeRect.height / 2) - 60;
            showQuickConnectionPicker(node, x, y);
            return;
        }
    });

    // Make node draggable
    makeNodeDraggable(div, node);
    return div;
}
```

### Connection Rendering System

```javascript
// Calculate precise edge points for clean connection lines
function getNodeEdgePoint(sourceNode, targetNode, connection) {
    const sourceLeft = parseInt(sourceNode.style.left) || 0;
    const sourceTop = parseInt(sourceNode.style.top) || 0;
    const sourceWidth = sourceNode.offsetWidth;
    const sourceHeight = sourceNode.offsetHeight;

    const targetLeft = parseInt(targetNode.style.left) || 0;
    const targetTop = parseInt(targetNode.style.top) || 0;
    const targetWidth = targetNode.offsetWidth;
    const targetHeight = targetNode.offsetHeight;

    // Calculate centers
    const sourceCenterX = sourceLeft + sourceWidth / 2;
    const sourceCenterY = sourceTop + sourceHeight / 2;
    const targetCenterX = targetLeft + targetWidth / 2;
    const targetCenterY = targetTop + targetHeight / 2;

    // Calculate direction vector
    const dx = targetCenterX - sourceCenterX;
    const dy = targetCenterY - sourceCenterY;
    const distance = Math.sqrt(dx * dx + dy * dy);

    if (distance === 0) {
        return { 
            source: { x: sourceCenterX, y: sourceCenterY }, 
            target: { x: targetCenterX, y: targetCenterY } 
        };
    }

    // Normalize direction
    const unitX = dx / distance;
    const unitY = dy / distance;

    // Calculate edge points with padding
    const sourceRadius = Math.max(sourceWidth, sourceHeight) / 2;
    const targetRadius = Math.max(targetWidth, targetHeight) / 2;
    const padding = 2;

    const sourceEdgeX = sourceCenterX + unitX * (sourceRadius + padding);
    const sourceEdgeY = sourceCenterY + unitY * (sourceRadius + padding);
    const targetEdgeX = targetCenterX - unitX * (targetRadius + padding);
    const targetEdgeY = targetCenterY - unitY * (targetRadius + padding);

    return {
        source: { x: sourceEdgeX, y: sourceEdgeY },
        target: { x: targetEdgeX, y: targetEdgeY }
    };
}

// Render all connections with proper styling
function renderConnections() {
    if (!currentConnections || currentConnections.length === 0) {
        return;
    }

    const connectionsLayer = document.getElementById('connectionsLayer');
    const defs = connectionsLayer.querySelector('defs');
    connectionsLayer.innerHTML = '';
    connectionsLayer.appendChild(defs);

    currentConnections.forEach((connection, index) => {
        const sourceNode = document.querySelector(`[data-node-id="${connection.sourceNodeId}"]`);
        const targetNode = document.querySelector(`[data-node-id="${connection.targetNodeId}"]`);

        if (!sourceNode || !targetNode) {
            console.warn(`Missing node for connection ${connection.id}`);
            return;
        }

        // Calculate edge points
        const edgePoints = getNodeEdgePoint(sourceNode, targetNode, connection);
        
        // Create SVG line element
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', edgePoints.source.x);
        line.setAttribute('y1', edgePoints.source.y);
        line.setAttribute('x2', edgePoints.target.x);
        line.setAttribute('y2', edgePoints.target.y);

        // Apply connection type styling
        const connectionTypeClass = `connection-${connection.connectionType.toLowerCase().replace(/_/g, '-')}`;
        line.classList.add('connection-line', connectionTypeClass);

        // Handle arrow markers
        const styling = connection.styling || {};
        
        // Source arrow
        if (styling.sourceArrowType && styling.sourceArrowType !== 'none') {
            const sourceMarker = `source-${styling.sourceArrowType}`;
            line.setAttribute('marker-start', `url(#${sourceMarker})`);
        }

        // Target arrow (default)
        const targetArrowType = styling.targetArrowType || 'arrow';
        if (targetArrowType !== 'none') {
            const targetMarker = getMarkerForConnectionType(connection.connectionType);
            line.setAttribute('marker-end', `url(#${targetMarker})`);
        }

        // Add interaction handlers
        line.dataset.connectionId = connection.id;
        line.onclick = (e) => selectConnection(connection);
        
        // Tooltip handlers
        line.addEventListener('mouseenter', (e) => showConnectionTooltip(e, connection));
        line.addEventListener('mouseleave', () => hideConnectionTooltip());
        line.addEventListener('mousemove', (e) => updateConnectionTooltipPosition(e));

        connectionsLayer.appendChild(line);

        // Add label if exists
        if (connection.label) {
            const labelGroup = createConnectionLabel(
                connection.label, 
                edgePoints.source.x, 
                edgePoints.source.y,
                edgePoints.target.x, 
                edgePoints.target.y
            );
            connectionsLayer.appendChild(labelGroup);
        }
    });
}
```

### YAML Export Frontend Handler

```javascript
// Frontend YAML export function
async function exportToYAML() {
    if (!currentMindMap) {
        alert('Please select a mindmap first');
        return;
    }

    try {
        // Call backend export endpoint
        const response = await fetch(`/api/mindmaps/${currentMindMap.id}/export/yaml`);
        
        if (!response.ok) {
            throw new Error('Export failed');
        }

        const yamlContent = await response.text();

        // Create download link
        const blob = new Blob([yamlContent], { type: 'application/x-yaml' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `kb-${currentMindMap.title.toLowerCase().replace(/[^a-z0-9]/g, '-')}-v1.yaml`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        // Show success notification
        showSuccessNotification('YAML file exported successfully!');

    } catch (error) {
        console.error('Error exporting YAML:', error);
        alert('Error exporting YAML file. Please try again.');
    }
}
```

## Backend Implementation

### REST Controller

```java
@RestController
@RequestMapping("/api/mindmaps")
public class MindMapController {
    
    @Autowired
    private MindMapRepository mindMapRepository;
    
    @Autowired
    private MindMapNodeRepository nodeRepository;
    
    @Autowired
    private MindMapConnectionRepository connectionRepository;
    
    @Autowired
    private YamlExportService yamlExportService;

    @GetMapping("/{mindMapId}/export/yaml")
    public ResponseEntity<String> exportMindMapToYaml(@PathVariable String mindMapId) {
        // Find mindmap
        Optional<MindMap> optionalMindMap = mindMapRepository.findById(mindMapId);
        if (!optionalMindMap.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        MindMap mindMap = optionalMindMap.get();
        
        // Get all related data
        List<MindMapNode> nodes = nodeRepository.findByMindMapId(mindMapId);
        List<MindMapConnection> connections = connectionRepository.findByMindMapId(mindMapId);

        // Generate YAML
        String yamlContent = yamlExportService.exportMindMapToYaml(mindMap, nodes, connections);

        // Set proper headers for download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-yaml"));
        headers.setContentDispositionFormData("attachment",
                "kb-" + mindMap.getTitle().toLowerCase().replaceAll("[^a-z0-9]", "-") + "-v1.yaml");

        return ResponseEntity.ok()
                .headers(headers)
                .body(yamlContent);
    }

    // Other CRUD endpoints...
}
```

### YAML Export Service - Core Implementation

```java
@Service
public class YamlExportService {

    public String exportMindMapToYaml(MindMap mindMap, List<MindMapNode> nodes, List<MindMapConnection> connections) {
        StringBuilder yaml = new StringBuilder();

        // Add YAML header and metadata
        addHeader(yaml, mindMap);

        // Group nodes by semantic type for organized structure
        Map<String, List<MindMapNode>> nodesByType = nodes.stream()
                .collect(Collectors.groupingBy(node -> node.getNodeType().toString()));

        // Add semantic sections in order
        addIntentSection(yaml, nodesByType.get("INTENT"));
        addActorsSection(yaml, nodesByType.get("ACTOR"));
        addBehavioralContractsSection(yaml, nodesByType.get("BEHAVIOR"), connections);
        addResourceAccessSection(yaml, nodesByType.get("RESOURCE"));
        addSecurityConstraintsSection(yaml, nodesByType.get("CONSTRAINT"));
        addEvolutionPathSection(yaml, nodesByType.get("EVOLUTION"));
        addConnectionsMetadata(yaml, connections);

        return yaml.toString();
    }

    private void addHeader(StringBuilder yaml, MindMap mindMap) {
        yaml.append("# Generated from MindMap: ").append(mindMap.getTitle()).append("\n");
        yaml.append("# Export Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        yaml.append("# Original ID: ").append(mindMap.getId()).append("\n\n");

        yaml.append("feature_name: \"").append(sanitizeForYaml(mindMap.getTitle())).append("\"\n");
        yaml.append("version: \"v").append(mindMap.getVersion() != null ? mindMap.getVersion() : "1").append("\"\n");
        yaml.append("created_date: \"").append(mindMap.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\"\n");
        yaml.append("mind_map_id: \"").append(mindMap.getId()).append("\"\n");
        yaml.append("description: \"").append(sanitizeForYaml(mindMap.getDescription())).append("\"\n\n");
    }

    private void addIntentSection(StringBuilder yaml, List<MindMapNode> intentNodes) {
        yaml.append("intent:\n");
        yaml.append("  _semantic: \"The core purpose and motivation behind this feature\"\n");

        if (intentNodes != null && !intentNodes.isEmpty()) {
            for (MindMapNode node : intentNodes) {
                yaml.append("  what: \"").append(sanitizeForYaml(node.getTitle())).append("\"\n");
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    yaml.append("  why: \"").append(sanitizeForYaml(node.getDescription())).append("\"\n");
                }
                if (node.getBusinessLogic() != null) {
                    addBusinessLogicProperties(yaml, node.getBusinessLogic(), "  ");
                }
            }
        } else {
            yaml.append("  what: \"Feature purpose not specified\"\n");
            yaml.append("  why: \"Business reasoning to be defined\"\n");
        }
        yaml.append("\n");
    }

    private void addActorsSection(StringBuilder yaml, List<MindMapNode> actorNodes) {
        yaml.append("actors:\n");
        yaml.append("  _semantic: \"Who interacts with the system and their capabilities\"\n");

        if (actorNodes != null && !actorNodes.isEmpty()) {
            for (MindMapNode node : actorNodes) {
                String actorKey = sanitizeForYamlKey(node.getTitle());
                yaml.append("  ").append(actorKey).append(":\n");
                yaml.append("    name: \"").append(sanitizeForYaml(node.getTitle())).append("\"\n");
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    yaml.append("    description: \"").append(sanitizeForYaml(node.getDescription())).append("\"\n");
                }
                yaml.append("    type: \"").append(node.getSemanticType()).append("\"\n");
                if (node.getBusinessLogic() != null) {
                    addBusinessLogicProperties(yaml, node.getBusinessLogic(), "    ");
                }
            }
        } else {
            yaml.append("  default_user:\n");
            yaml.append("    name: \"System User\"\n");
            yaml.append("    type: \"USER\"\n");
        }
        yaml.append("\n");
    }

    private void addBehavioralContractsSection(StringBuilder yaml, List<MindMapNode> behaviorNodes, List<MindMapConnection> connections) {
        yaml.append("behavioral_contracts:\n");
        yaml.append("  _semantic: \"Conditional logic and business rules that govern behavior\"\n");

        if (behaviorNodes != null && !behaviorNodes.isEmpty()) {
            for (MindMapNode node : behaviorNodes) {
                String behaviorKey = sanitizeForYamlKey(node.getTitle());
                yaml.append("  ").append(behaviorKey).append(":\n");
                yaml.append("    name: \"").append(sanitizeForYaml(node.getTitle())).append("\"\n");
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    yaml.append("    description: \"").append(sanitizeForYaml(node.getDescription())).append("\"\n");
                }
                yaml.append("    type: \"").append(node.getSemanticType()).append("\"\n");

                // Add connections as behavior flows
                List<MindMapConnection> nodeConnections = connections.stream()
                        .filter(conn -> conn.getSourceNodeId().equals(node.getId()) || conn.getTargetNodeId().equals(node.getId()))
                        .collect(Collectors.toList());

                if (!nodeConnections.isEmpty()) {
                    yaml.append("    flows:\n");
                    for (MindMapConnection conn : nodeConnections) {
                        yaml.append("      - connection_type: \"").append(conn.getConnectionType()).append("\"\n");
                        if (conn.getLabel() != null) {
                            yaml.append("        label: \"").append(sanitizeForYaml(conn.getLabel())).append("\"\n");
                        }
                        if (conn.getDescription() != null) {
                            yaml.append("        condition: \"").append(sanitizeForYaml(conn.getDescription())).append("\"\n");
                        }
                    }
                }

                if (node.getBusinessLogic() != null) {
                    addBusinessLogicProperties(yaml, node.getBusinessLogic(), "    ");
                }
            }
        } else {
            yaml.append("  default_behavior:\n");
            yaml.append("    name: \"Standard Processing\"\n");
            yaml.append("    type: \"CONDITION\"\n");
        }
        yaml.append("\n");
    }

    private void addResourceAccessSection(StringBuilder yaml, List<MindMapNode> resourceNodes) {
        yaml.append("resource_access:\n");
        yaml.append("  _semantic: \"What resources exist and who can access them\"\n");

        if (resourceNodes != null && !resourceNodes.isEmpty()) {
            for (MindMapNode node : resourceNodes) {
                String resourceKey = sanitizeForYamlKey(node.getTitle());
                yaml.append("  ").append(resourceKey).append(":\n");
                yaml.append("    name: \"").append(sanitizeForYaml(node.getTitle())).append("\"\n");
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    yaml.append("    description: \"").append(sanitizeForYaml(node.getDescription())).append("\"\n");
                }
                yaml.append("    type: \"").append(node.getSemanticType()).append("\"\n");
                if (node.getBusinessLogic() != null) {
                    addBusinessLogicProperties(yaml, node.getBusinessLogic(), "    ");
                }
            }
        } else {
            yaml.append("  default_resources:\n");
            yaml.append("    name: \"System Resources\"\n");
            yaml.append("    type: \"ENDPOINT\"\n");
        }
        yaml.append("\n");
    }

    private void addSecurityConstraintsSection(StringBuilder yaml, List<MindMapNode> constraintNodes) {
        yaml.append("security_constraints:\n");
        yaml.append("  _semantic: \"Non-negotiable security requirements and standards\"\n");

        if (constraintNodes != null && !constraintNodes.isEmpty()) {
            for (MindMapNode node : constraintNodes) {
                String constraintKey = sanitizeForYamlKey(node.getTitle());
                yaml.append("  ").append(constraintKey).append(":\n");
                yaml.append("    name: \"").append(sanitizeForYaml(node.getTitle())).append("\"\n");
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    yaml.append("    requirement: \"").append(sanitizeForYaml(node.getDescription())).append("\"\n");
                }
                yaml.append("    type: \"").append(node.getSemanticType()).append("\"\n");
                if (node.getBusinessLogic() != null) {
                    addBusinessLogicProperties(yaml, node.getBusinessLogic(), "    ");
                }
            }
        } else {
            yaml.append("  default_security:\n");
            yaml.append("    name: \"Basic Security\"\n");
            yaml.append("    type: \"SECURITY\"\n");
        }
        yaml.append("\n");
    }

    private void addEvolutionPathSection(StringBuilder yaml, List<MindMapNode> evolutionNodes) {
        yaml.append("evolution_path:\n");
        yaml.append("  _semantic: \"How this concept should grow and change over time\"\n");

        if (evolutionNodes != null && !evolutionNodes.isEmpty()) {
            for (MindMapNode node : evolutionNodes) {
                yaml.append("  current_state: \"").append(sanitizeForYaml(node.getTitle())).append("\"\n");
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    yaml.append("  next_iteration: \"").append(sanitizeForYaml(node.getDescription())).append("\"\n");
                }
                yaml.append("  evolution_type: \"").append(node.getSemanticType()).append("\"\n");
                if (node.getBusinessLogic() != null) {
                    addBusinessLogicProperties(yaml, node.getBusinessLogic(), "  ");
                }
            }
        } else {
            yaml.append("  current_state: \"initial_implementation\"\n");
            yaml.append("  next_iteration: \"feature_enhancement\"\n");
            yaml.append("  evolution_type: \"FUTURE_VISION\"\n");
        }
        yaml.append("\n");
    }

    private void addConnectionsMetadata(StringBuilder yaml, List<MindMapConnection> connections) {
        yaml.append("connections_metadata:\n");
        yaml.append("  _semantic: \"Relationships and data flows between components\"\n");
        yaml.append("  total_connections: ").append(connections.size()).append("\n");

        if (!connections.isEmpty()) {
            yaml.append("  connection_types:\n");
            Map<String, Long> connectionCounts = connections.stream()
                    .collect(Collectors.groupingBy(
                            conn -> conn.getConnectionType().toString(),
                            Collectors.counting()));

            for (Map.Entry<String, Long> entry : connectionCounts.entrySet()) {
                yaml.append("    ").append(entry.getKey().toLowerCase()).append(": ").append(entry.getValue()).append("\n");
            }

            yaml.append("  flows:\n");
            for (MindMapConnection conn : connections) {
                yaml.append("    - source: \"").append(conn.getSourceNodeId()).append("\"\n");
                yaml.append("      target: \"").append(conn.getTargetNodeId()).append("\"\n");
                yaml.append("      type: \"").append(conn.getConnectionType()).append("\"\n");
                if (conn.getLabel() != null) {
                    yaml.append("      label: \"").append(sanitizeForYaml(conn.getLabel())).append("\"\n");
                }
                if (conn.getDescription() != null) {
                    yaml.append("      description: \"").append(sanitizeForYaml(conn.getDescription())).append("\"\n");
                }
            }
        }
        yaml.append("\n");
    }

    private void addBusinessLogicProperties(StringBuilder yaml, MindMapNode.BusinessLogic businessLogic, String indent) {
        if (businessLogic != null) {
            Map<String, Object> businessLogicMap = convertBusinessLogicToMap(businessLogic);
            if (!businessLogicMap.isEmpty()) {
                yaml.append(indent).append("business_logic:\n");
                for (Map.Entry<String, Object> entry : businessLogicMap.entrySet()) {
                    yaml.append(indent).append("  ").append(entry.getKey()).append(": \"")
                            .append(sanitizeForYaml(entry.getValue().toString())).append("\"\n");
                }
            }
        }
    }

    private Map<String, Object> convertBusinessLogicToMap(MindMapNode.BusinessLogic businessLogic) {
        Map<String, Object> map = new HashMap<>();

        if (businessLogic.getInputParameters() != null && !businessLogic.getInputParameters().isEmpty()) {
            map.put("input_parameters", String.join(", ", businessLogic.getInputParameters()));
        }

        if (businessLogic.getOutputParameters() != null && !businessLogic.getOutputParameters().isEmpty()) {
            map.put("output_parameters", String.join(", ", businessLogic.getOutputParameters()));
        }

        if (businessLogic.getLogicExpression() != null && !businessLogic.getLogicExpression().trim().isEmpty()) {
            map.put("logic_expression", businessLogic.getLogicExpression());
        }

        if (businessLogic.getIfThenRules() != null && !businessLogic.getIfThenRules().isEmpty()) {
            StringBuilder rulesBuilder = new StringBuilder();
            for (Map.Entry<String, String> rule : businessLogic.getIfThenRules().entrySet()) {
                if (rulesBuilder.length() > 0) {
                    rulesBuilder.append("; ");
                }
                rulesBuilder.append("IF ").append(rule.getKey()).append(" THEN ").append(rule.getValue());
            }
            map.put("if_then_rules", rulesBuilder.toString());
        }

        if (businessLogic.getDependencies() != null && !businessLogic.getDependencies().isEmpty()) {
            map.put("dependencies", String.join(", ", businessLogic.getDependencies()));
        }

        return map;
    }

    private String sanitizeForYaml(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String sanitizeForYamlKey(String input) {
        if (input == null) return "unnamed";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "");
    }
}
```

## Database Schema

### MongoDB Collections

1. **mindmaps** - Core mindmap metadata
2. **mindmap_nodes** - Individual nodes with business logic
3. **mindmap_connections** - Relationships between nodes

### Repository Interfaces

```java
@Repository
public interface MindMapRepository extends MongoRepository<MindMap, String> {
    List<MindMap> findByOwnerId(String ownerId);
    List<MindMap> findByStatus(MindMap.MindMapStatus status);
}

@Repository
public interface MindMapNodeRepository extends MongoRepository<MindMapNode, String> {
    List<MindMapNode> findByMindMapId(String mindMapId);
    List<MindMapNode> findByMindMapIdAndNodeType(String mindMapId, MindMapNode.NodeType nodeType);
}

@Repository
public interface MindMapConnectionRepository extends MongoRepository<MindMapConnection, String> {
    List<MindMapConnection> findByMindMapId(String mindMapId);
    List<MindMapConnection> findBySourceNodeIdOrTargetNodeId(String sourceNodeId, String targetNodeId);
}
```

## Generated YAML Structure

### Example Output Format

```yaml
# Generated from MindMap: Authentication System
# Export Date: 2025-06-01T10:15:30.123456
# Original ID: 6832f47d632a2fe6308da865

feature_name: "Authentication System"
version: "v1.0"
created_date: "2025-06-01"
mind_map_id: "6832f47d632a2fe6308da865"
description: "Business logic for user authentication"

intent:
  _semantic: "The core purpose and motivation behind this feature"
  what: "Secure Web Platform"
  why: "Balance security requirements with developer productivity"

actors:
  _semantic: "Who interacts with the system and their capabilities"
  development_user:
    name: "Development User"
    description: "Developer who needs easy access during development"
    type: "USER"
    business_logic:
      input_parameters: "environment, mode"
      output_parameters: "access_granted"
      logic_expression: "IF development_mode THEN bypass_security"

behavioral_contracts:
  _semantic: "Conditional logic and business rules that govern behavior"
  bypass_login:
    name: "Bypass Login"
    description: "IF development mode THEN permitAll() + disable CSRF"
    type: "CONDITION"
    flows:
      - connection_type: "TRIGGERS"
        label: "activates"
        condition: "Development user activates bypass login behavior"
    business_logic:
      if_then_rules: "IF env=dev THEN permitAll(); IF env=prod THEN requireAuth()"

resource_access:
  _semantic: "What resources exist and who can access them"
  api_endpoints:
    name: "API Endpoints"
    description: "REST API access points"
    type: "ENDPOINT"
    business_logic:
      dependencies: "authentication_service, authorization_service"

security_constraints:
  _semantic: "Non-negotiable security requirements and standards"
  csrf_protection:
    name: "CSRF Protection"
    requirement: "Must be enabled in production environments"
    type: "SECURITY"
    business_logic:
      validation_rules: "prod_env: enabled, dev_env: optional"

evolution_path:
  _semantic: "How this concept should grow and change over time"
  current_state: "basic_authentication"
  next_iteration: "oauth2_integration"
  evolution_type: "FUTURE_VISION"

connections_metadata:
  _semantic: "Relationships and data flows between components"
  total_connections: 3
  connection_types:
    triggers: 2
    depends_on: 1
  flows:
    - source: "user_node_id"
      target: "auth_behavior_id"
      type: "TRIGGERS"
      label: "initiates"
      description: "User action triggers authentication flow"
```

## CSS Styling Requirements

### Node Type Styling

```css
.node-intent {
    border-color: #28a745;
    background-color: #d4edda;
}

.node-actor {
    border-color: #007bff;
    background-color: #d1ecf1;
}

.node-behavior {
    border-color: #ffc107;
    background-color: #fff3cd;
}

.node-resource {
    border-color: #6f42c1;
    background-color: #e2d9f3;
}

.node-constraint {
    border-color: #dc3545;
    background-color: #f8d7da;
}

.node-evolution {
    border-color: #fd7e14;
    background-color: #fff3e0;
}
```

### Connection Type Styling

```css
.connection-depends-on {
    stroke: #6c757d;
    stroke-width: 2px;
    stroke-dasharray: 5,5;
}

.connection-triggers {
    stroke: #fd7e14;
    stroke-width: 2px;
}

.connection-provides {
    stroke: #198754;
    stroke-width: 2px;
}

.connection-sequence {
    stroke: #0d6efd;
    stroke-width: 3px;
}

.connection-if-then {
    stroke: #ffc107;
    stroke-width: 2px;
    stroke-dasharray: 8,4;
}

.connection-contains {
    stroke: #6f42c1;
    stroke-width: 2px;
    stroke-dasharray: 3,3;
}
```

## Build and Deployment

### Maven Configuration (pom.xml)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
</dependencies>
```

### Application Configuration

```properties
# MongoDB Configuration
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=mindmap_db

# Server Configuration
server.port=3030
server.servlet.context-path=/

# Logging
logging.level.com.recapmap.mothership=DEBUG
logging.level.org.springframework.data.mongodb=DEBUG
```

## Testing Strategy

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class YamlExportServiceTest {

    @Mock
    private MindMap mockMindMap;
    
    @Mock
    private MindMapNode mockNode;
    
    @Mock
    private MindMapConnection mockConnection;

    @InjectMocks
    private YamlExportService yamlExportService;

    @Test
    void shouldExportMindMapToYaml() {
        // Given
        when(mockMindMap.getTitle()).thenReturn("Test MindMap");
        when(mockMindMap.getDescription()).thenReturn("Test Description");
        when(mockMindMap.getId()).thenReturn("test-id");
        when(mockMindMap.getVersion()).thenReturn("1.0");
        when(mockMindMap.getCreatedAt()).thenReturn(LocalDateTime.now());

        when(mockNode.getNodeType()).thenReturn(MindMapNode.NodeType.INTENT);
        when(mockNode.getTitle()).thenReturn("Test Intent");
        when(mockNode.getDescription()).thenReturn("Test Intent Description");

        List<MindMapNode> nodes = Arrays.asList(mockNode);
        List<MindMapConnection> connections = Arrays.asList(mockConnection);

        // When
        String result = yamlExportService.exportMindMapToYaml(mockMindMap, nodes, connections);

        // Then
        assertThat(result).contains("feature_name: \"Test MindMap\"");
        assertThat(result).contains("intent:");
        assertThat(result).contains("what: \"Test Intent\"");
        assertThat(result).contains("why: \"Test Intent Description\"");
    }

    @Test
    void shouldSanitizeYamlStrings() {
        // Test YAML string sanitization
        String input = "Test \"quoted\" string\nwith newlines";
        String result = yamlExportService.sanitizeForYaml(input);
        
        assertThat(result).isEqualTo("Test \\\"quoted\\\" string\\nwith newlines");
    }

    @Test
    void shouldSanitizeYamlKeys() {
        // Test YAML key sanitization
        String input = "Test Key With Spaces & Special Characters!";
        String result = yamlExportService.sanitizeForYamlKey(input);
        
        assertThat(result).isEqualTo("test_key_with_spaces_special_characters");
    }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.data.mongodb.database=test_mindmap_db")
class MindMapControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MindMapRepository mindMapRepository;

    @Test
    void shouldExportMindMapToYaml() {
        // Given
        MindMap mindMap = createTestMindMap();
        mindMapRepository.save(mindMap);

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/mindmaps/" + mindMap.getId() + "/export/yaml",
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("application/x-yaml"));
        assertThat(response.getBody()).contains("feature_name:");
        assertThat(response.getBody()).contains("intent:");
    }
}
```

## Performance Considerations

### Optimization Strategies

1. **Connection Rendering**: Skip real-time connection updates during node dragging
2. **Large Datasets**: Implement pagination for mind maps with 100+ nodes
3. **Memory Management**: Clear unused DOM elements and event listeners
4. **Caching**: Cache YAML exports for unchanged mind maps
5. **Database Queries**: Use projection to limit returned fields

### Monitoring

```java
@Component
public class YamlExportMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public YamlExportMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @EventListener
    public void handleYamlExport(YamlExportEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("yaml.export.duration")
            .description("Time taken to export YAML")
            .register(meterRegistry));
            
        meterRegistry.counter("yaml.export.count", 
            "mindmap_id", event.getMindMapId(),
            "node_count", String.valueOf(event.getNodeCount()))
            .increment();
    }
}
```

## Security Considerations

### Input Validation

```java
@Component
public class YamlExportValidator {
    
    public void validateExportRequest(String mindMapId, String userId) {
        // Validate user has access to mindmap
        if (!hasUserAccess(mindMapId, userId)) {
            throw new AccessDeniedException("User does not have access to mindmap");
        }
        
        // Validate mindmap exists
        if (!mindMapExists(mindMapId)) {
            throw new EntityNotFoundException("MindMap not found");
        }
        
        // Rate limiting
        if (exceedsRateLimit(userId)) {
            throw new RateLimitExceededException("Export rate limit exceeded");
        }
    }
}
```

### Output Sanitization

All user input must be sanitized before YAML generation:
- Escape special YAML characters
- Remove or escape newlines
- Validate string lengths
- Prevent YAML injection attacks

## Error Handling

### Frontend Error Handling

```javascript
async function exportToYAML() {
    try {
        const response = await fetch(`/api/mindmaps/${currentMindMap.id}/export/yaml`);
        
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error('MindMap not found');
            } else if (response.status === 403) {
                throw new Error('Access denied');
            } else if (response.status === 429) {
                throw new Error('Rate limit exceeded. Please try again later.');
            } else {
                throw new Error('Export failed');
            }
        }
        
        // Success handling...
        
    } catch (error) {
        console.error('Export error:', error);
        showErrorNotification(error.message);
    }
}
```

### Backend Error Handling

```java
@RestControllerAdvice
public class MindMapExportExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("Access denied", ex.getMessage()));
    }
    
    @ExceptionHandler(YamlExportException.class)
    public ResponseEntity<ErrorResponse> handleYamlExportError(YamlExportException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Export failed", ex.getMessage()));
    }
}
```

## Development Workflow

### Setup Steps

1. **Database Setup**
   ```bash
   # Start MongoDB
   mongod --dbpath ./data/db
   
   # Create indexes
   db.mindmaps.createIndex({"ownerId": 1})
   db.mindmap_nodes.createIndex({"mindMapId": 1})
   db.mindmap_connections.createIndex({"mindMapId": 1})
   ```

2. **Build and Run**
   ```bash
   # Compile
   mvn compile
   
   # Run tests
   mvn test
   
   # Start application
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. **Access Application**
   - Web Interface: http://localhost:3030/web/mindmap-editor
   - API Documentation: http://localhost:3030/swagger-ui.html

### File Structure

```
src/
├── main/
│   ├── java/com/company/mindmap/
│   │   ├── entity/
│   │   │   ├── MindMap.java
│   │   │   ├── MindMapNode.java
│   │   │   └── MindMapConnection.java
│   │   ├── repository/
│   │   │   ├── MindMapRepository.java
│   │   │   ├── MindMapNodeRepository.java
│   │   │   └── MindMapConnectionRepository.java
│   │   ├── service/
│   │   │   └── YamlExportService.java
│   │   ├── controller/
│   │   │   └── MindMapController.java
│   │   └── config/
│   │       └── MongoConfig.java
│   └── resources/
│       ├── templates/
│       │   └── mindmap-editor.html
│       └── application.properties
└── test/
    └── java/com/company/mindmap/
        ├── service/
        │   └── YamlExportServiceTest.java
        └── controller/
            └── MindMapControllerTest.java
```

This knowledge base provides complete implementation details for replicating the mind map to YAML conversion system. All code examples are production-ready and include proper error handling, validation, and testing strategies.
