# UI/UX Design Specifications: Floating Widget Dashboard

**Date**: May 31, 2025  
**Project**: RecapMap Core React Frontend  
**Purpose**: Complete interface design and user experience specifications

## Design Philosophy

We are building a **desktop-first mission control center** where users orchestrate complex business logic through multiple floating interfaces. The design prioritizes professional productivity tools over consumer simplicity, similar to advanced DAWs (Digital Audio Workstations) or video editing software.

## Platform Mode Architecture

### Desktop Mode (Primary)
- **Full Feature Set**: Complete Builder mode with all functionality
- **Floating Widget Dashboard**: Multiple draggable panels and interfaces
- **Professional UX**: Optimized for complex workflow design
- **Multi-Monitor Support**: Widgets can span across multiple displays
- **Keyboard Shortcuts**: Power user efficiency features

### Mobile Mode (Separate)
- **Mode Toggle**: Users can switch between modes on mobile devices
- **Generated App Focus**: Primarily for using created applications
- **Basic Chat**: Simple interface for AI conversation and app generation
- **Touch-Optimized**: Completely different UI paradigm
- **No Responsive Bridge**: Clean separation, no compromised middle ground

## Canvas and Layout System

### Main Canvas Area
- **React Flow Integration**: Core visual editor with 8-node support
- **Infinite Canvas**: Zoom and pan with smooth performance
- **Grid System**: Toggle-able snap-to-grid for alignment
- **Zoom Levels**: 25% to 400% zoom with node readability at all levels
- **Background**: Subtle dot grid pattern with zoom-responsive scaling

### Grid-Based Layout Management
- **Snap-to-Grid Toggle**: Users can enable/disable grid alignment
- **Smart Spacing**: Automatic spacing suggestions for clean layouts
- **Alignment Helpers**: Visual guides for node alignment and distribution
- **No Responsive Constraints**: Fixed desktop layout, no mobile compromises

### Zoom and Navigation
- **Mouse Wheel Zoom**: Standard zoom in/out functionality
- **Minimap**: Overview panel showing full canvas with viewport indicator
- **Fit View**: One-click to fit all nodes in viewport
- **Zoom to Selection**: Focus on selected nodes

## 8-Node Visual Design System

### Uniform Node Sizing
- **Standard Dimensions**: 160px width × 120px height for all node types
- **Consistent Proportions**: Maintains readability across zoom levels
- **No Size Variations**: All 8 node types use identical dimensions

### Visual Differentiation System
- **Color Coding**: Primary differentiation method
  - Base Node: N/A (abstract)
  - Use Case: Blue (#3B82F6)
  - Screen: Green (#10B981)
  - User: Orange (#F59E0B)
  - Process/Tool: Purple (#8B5CF6)
  - Storage: Yellow (#EAB308)
  - Flow Controller: Red (#EF4444)
  - Error/Rejection: Gray (#6B7280)

- **Icon System**: Secondary differentiation for zoom-out recognition
  - Use Case: Target/Bullseye
  - Screen: Monitor/Display
  - User: Person/User
  - Process/Tool: Gear/Cog
  - Storage: Database/Cylinder
  - Flow Controller: Diamond/Switch
  - Error/Rejection: Warning/Alert

### Node Structure Layout
```
┌─────────────────────────────────┐
│ [Icon] NODE_TYPE               │ ← Header with type + icon
├─────────────────────────────────┤
│ Node Title                      │ ← Main content area
│ Brief description or            │
│ key properties                  │
│                                 │
└─────────────────────────────────┘
  ↑                             ↑
Input Handle                Output Handle
```

### Connection System
- **Input Handle**: Top center of each node
- **Output Handle**: Bottom center of each node
- **Connection Lines**: Bezier curves with color coding based on source node
- **Connection Validation**: Visual feedback for valid/invalid connections
- **Multi-Connection Support**: Nodes can have multiple inputs and outputs

## Floating Panel Architecture

### Panel Management System
- **Windows 11 Style Overlapping**: Panels can overlap naturally
- **Z-Index Management**: Click to bring panel to front
- **Draggable Headers**: Click and drag from panel title bars
- **Resizable Panels**: Drag edges to resize (where appropriate)
- **Panel Docking**: Snap to screen edges and other panels

### Property Panel System
- **Context-Sensitive**: Panel content changes based on selected node type
- **Right-Side Default**: Panels appear on right side of selected node
- **Draggable Positioning**: Users can move panels anywhere on screen
- **Panel Persistence**: Panel positions saved per project/view state

### Panel Types and Specifications

#### Node Property Panel
- **Default Size**: 300px width × 400px height
- **Content**: Form fields specific to each node type
- **Sections**: Collapsible sections for complex nodes
- **Real-Time Updates**: Changes immediately reflect in node display
- **Validation Feedback**: Inline error messages and field validation

#### Chat Panel (Per Node)
- **Triggered By**: Right-click → "Chat about this node"
- **Size**: 320px width × 280px height
- **Isolated Context**: Each node gets its own chat context
- **Merge Capability**: Option to merge conversation to main chat
- **Field References**: Click form fields to mention them in chat

#### Main Chat Widget
- **Fixed Position**: Bottom of screen, draggable
- **Standard Size**: 300px width × 400px height (landscape ratio)
- **Always Visible**: Persistent across all workflow operations
- **Agent Selection**: @mention system for different AI agents
- **Node Mentions**: Click any node to reference it in chat

## Multi-User Collaboration Interface

### User Presence System
- **Live Cursors**: Real-time cursor positions of other users
- **User Avatars**: Small profile pictures showing who's online
- **Activity Indicators**: Visual feedback showing who's editing what
- **User List Panel**: Expandable list of active collaborators

### Conflict Resolution UI
- **Last Edit Wins**: Default behavior for mission-critical operations
- **Warning Icons**: Visual indicators when conflicts occur
- **Edit History Panel**: Expandable timeline showing recent changes
- **Conflict Details**: Hover/click warnings to see what changed
- **User Attribution**: Clear indication of who made each change

### Real-Time Sync Indicators
- **Sync Status**: Visual indicator showing connection status
- **Change Broadcasting**: Smooth animations for remote changes
- **Offline Capability**: Visual indication when working offline
- **Sync Queue**: Show pending changes waiting to sync

## Chat System Interface Design

### Multi-Agent Chat Architecture
- **Agent Identification**: Visual avatars/icons for different AI agents
- **@Mention System**: Type @ to see available agents
- **Context Switching**: Clear visual indication of chat scope changes
- **Conversation Threading**: Visual connection between related messages

### Chat Context Management
- **Scope Indicators**: Visual breadcrumbs showing current chat context
- **Context History**: Navigation between different chat scopes
- **Merge Operations**: UI for combining isolated chats with main chat
- **Reference Linking**: Visual connections between chat and referenced nodes

### Chat Timeline Tree (Future Feature)
- **Conversation Branching**: Visual tree showing chat evolution
- **Context Selection**: Click tree nodes to reference past conversations
- **Deep Thinking Support**: Multi-level conversation organization
- **Strategic Planning**: Visual organization for complex decision making

## View State Management

### Saved Views System
- **View Snapshots**: Save current panel arrangement and zoom level
- **Quick Switch**: Dropdown to rapidly change between saved views
- **Scenario Views**: Different arrangements for different collaboration types
- **War Room Mode**: High-density view for intensive collaboration

### Widget Arrangement Persistence
- **Per-Project Storage**: View states saved with each project
- **User Preferences**: Personal defaults for panel positioning
- **Team Templates**: Shared view arrangements for collaboration
- **Export/Import**: Share view configurations between projects

## Responsive Strategy (Desktop-Only)

### Multi-Monitor Support
- **Panel Distribution**: Widgets can be dragged across multiple monitors
- **Monitor Awareness**: Smart positioning based on available screen real estate
- **Fullscreen Modes**: Canvas can go fullscreen on secondary monitor

### Window Size Adaptations
- **Minimum Size**: 1366×768 minimum supported resolution
- **Panel Scaling**: Intelligent panel resizing for smaller screens
- **UI Density Options**: Compact/Normal/Spacious density settings
- **No Mobile Compromises**: Clean desktop-only design

## Performance Optimization

### Rendering Strategy
- **200+ Node Capacity**: Smooth performance up to 200 nodes
- **Viewport Culling**: Only render visible nodes during pan/zoom
- **Connection Optimization**: Efficient bezier curve rendering
- **Panel Virtualization**: Lazy load panel content when needed

### Future Migration Path
- **Unity/WebGL Target**: When performance limits reached
- **Component Reuse**: UI components designed for Unity migration
- **Data Model Consistency**: Same data structures in Unity implementation

## Accessibility Considerations

### Keyboard Navigation
- **Tab Order**: Logical tab progression through interface
- **Keyboard Shortcuts**: Power user hotkeys for common operations
- **Focus Indicators**: Clear visual feedback for keyboard focus
- **Screen Reader Support**: Semantic HTML and ARIA labels

### Visual Accessibility
- **High Contrast Options**: Alternative color themes for accessibility
- **Zoom Support**: UI scales cleanly with browser zoom
- **Color Blind Friendly**: Node icons provide non-color differentiation
- **Reduced Motion**: Respect prefers-reduced-motion settings

## Error States and Loading

### Loading States
- **Skeleton Screens**: Placeholder content while loading
- **Progressive Loading**: Nodes appear as data becomes available
- **Loading Indicators**: Clear feedback for long operations
- **Offline Indicators**: Visual feedback when backend unavailable

### Error Handling
- **Graceful Degradation**: Partial functionality when services unavailable
- **Error Boundaries**: Prevent crashes from propagating
- **User-Friendly Messages**: Clear explanations of what went wrong
- **Recovery Actions**: Specific steps users can take to resolve issues

## Animation and Transitions

### Micro-Interactions
- **Smooth Transitions**: 200-300ms transitions for state changes
- **Hover Effects**: Subtle feedback for interactive elements
- **Panel Animations**: Smooth sliding for panel open/close
- **Connection Animations**: Animated line drawing for new connections

### Visual Feedback
- **Selection States**: Clear indication of selected nodes/panels
- **Drag Feedback**: Visual preview during drag operations
- **Connection Preview**: Live preview while creating connections
- **Validation Feedback**: Immediate visual feedback for form validation

This UI/UX design creates a professional, powerful interface that scales from simple workflows to complex business logic modeling while maintaining usability and performance.

---

**Next Phase**: Implement this design system in React components with TailwindCSS styling and smooth animations.
