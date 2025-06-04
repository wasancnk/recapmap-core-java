# Coding Discipline and Standards

**Date**: May 31, 2025  
**Project**: RecapMap - Visual AGI Orchestration Platform  
**Component**: Development Standards and Code Quality Framework

## Overview

This document establishes the coding discipline, standards, and quality practices for RecapMap development. These standards ensure maintainable, scalable, and high-quality code that supports our ambitious vision of visual AGI orchestration while preventing the technical debt that could derail long-term success.

## Core Development Principles

### Code Quality Philosophy
```yaml
quality_principles:
  readability_first: "Code is read 10x more than it's written"
  explicit_over_clever: "Clarity trumps conciseness"
  type_safety: "TypeScript types prevent runtime errors"
  testability: "Every component should be easily testable"
  maintainability: "Code should survive team changes"
  
  zero_tolerance:
    - "No any types in production code"
    - "No console.log statements in commits"
    - "No TODO comments without issue numbers"
    - "No magic numbers or hardcoded strings"
    - "No component files over size limits"
```

### Production-Ready Standards
```yaml
production_standards:
  error_handling: "Every API call must handle errors gracefully"
  accessibility: "WCAG 2.1 AA compliance for all UI components"
  performance: "Sub-2s load times for standard operations"
  security: "Input validation and XSS prevention everywhere"
  monitoring: "Meaningful logs and metrics for debugging"
  
  enterprise_readiness:
    - "Configurable for different deployment environments"
    - "Comprehensive error tracking and alerting"
    - "Audit trails for all user actions"
    - "Data protection and privacy compliance"
    - "Scalable architecture patterns from day one"
```

## TypeScript Standards

### Type Definitions
```typescript
// ✅ GOOD: Explicit, descriptive types
interface MindmapNode {
  id: string;
  type: NodeType;
  position: Position;
  data: NodeData;
  connections: NodeConnection[];
  metadata: NodeMetadata;
}

interface Position {
  x: number;
  y: number;
}

interface NodeData {
  title: string;
  description?: string;
  properties: Record<string, unknown>;
  validation?: ValidationRules;
}

// ❌ BAD: Vague or overly permissive types
interface Node {
  id: any;
  data: any;
  position: any;
}
```

### Strict Type Enforcement
```typescript
// ✅ GOOD: Strict typing with proper error handling
const updateNodePosition = (
  nodeId: string, 
  position: Position
): Result<void, NodeError> => {
  if (!isValidNodeId(nodeId)) {
    return { success: false, error: new InvalidNodeIdError(nodeId) };
  }
  
  if (!isValidPosition(position)) {
    return { success: false, error: new InvalidPositionError(position) };
  }
  
  // Update logic here
  return { success: true, data: undefined };
};

// ❌ BAD: Loose typing and poor error handling
const updateNode = (id: any, pos: any) => {
  // Might fail silently or throw unexpected errors
  nodes[id].position = pos;
};
```

### Enum and Union Types
```typescript
// ✅ GOOD: Explicit enums for semantic clarity
enum NodeType {
  USE_CASE = 'use_case',
  SCREEN = 'screen',
  USER = 'user',
  PROCESS_TOOL = 'process_tool',
  STORAGE = 'storage',
  FLOW_CONTROLLER = 'flow_controller',
  ERROR_REJECTION = 'error_rejection'
}

// ✅ GOOD: Union types for constrained values
type PanelState = 'open' | 'closed' | 'minimized' | 'docked';
type ValidationLevel = 'error' | 'warning' | 'info';

// ❌ BAD: String literals without type safety
const nodeType = "use case"; // Typos not caught at compile time
```

## Component Architecture Standards

### Atomic Design Enforcement
```typescript
// ✅ GOOD: Atomic component structure
// atoms/Button.tsx (< 50 lines)
interface ButtonProps {
  variant: 'primary' | 'secondary' | 'danger';
  size: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  onClick: () => void;
  children: React.ReactNode;
  className?: string;
}

export const Button: React.FC<ButtonProps> = ({
  variant,
  size,
  disabled = false,
  loading = false,
  onClick,
  children,
  className = ''
}) => {
  const baseClasses = 'font-medium rounded-lg transition-colors';
  const variantClasses = {
    primary: 'bg-blue-600 hover:bg-blue-700 text-white',
    secondary: 'bg-gray-200 hover:bg-gray-300 text-gray-900',
    danger: 'bg-red-600 hover:bg-red-700 text-white'
  };
  
  return (
    <button
      className={`${baseClasses} ${variantClasses[variant]} ${className}`}
      disabled={disabled || loading}
      onClick={onClick}
      aria-disabled={disabled || loading}
    >
      {loading ? <LoadingSpinner /> : children}
    </button>
  );
};
```

### Component Size Limits
```yaml
component_size_limits:
  atoms: 
    max_lines: 50
    max_props: 8
    complexity: "Single responsibility only"
    
  molecules:
    max_lines: 100
    max_props: 12
    complexity: "Simple composition of atoms"
    
  organisms:
    max_lines: 200
    max_props: 15
    complexity: "Business logic allowed, use custom hooks"
    
  templates:
    max_lines: 150
    max_props: 10
    complexity: "Layout only, no business logic"
    
  pages:
    max_lines: 100
    max_props: 5
    complexity: "Composition only, extract logic to hooks"

enforcement_rules:
  automated_checking: "ESLint rules enforce line limits"
  code_review: "Mandatory review for components near limits"
  refactoring_triggers: "Automatic warnings at 80% of limits"
  extraction_guidelines: "Clear patterns for splitting components"
```

### Custom Hooks Standards
```typescript
// ✅ GOOD: Well-structured custom hook
interface UseNodeEditorOptions {
  nodeId: string;
  autoSave?: boolean;
  validationLevel?: ValidationLevel;
}

interface UseNodeEditorReturn {
  node: MindmapNode | null;
  isLoading: boolean;
  error: Error | null;
  isDirty: boolean;
  
  // Actions
  updateProperty: (key: string, value: unknown) => void;
  saveChanges: () => Promise<Result<void, SaveError>>;
  resetChanges: () => void;
  deleteNode: () => Promise<Result<void, DeleteError>>;
  
  // Validation
  validateNode: () => ValidationResult;
  getValidationErrors: () => ValidationError[];
}

export const useNodeEditor = ({
  nodeId,
  autoSave = false,
  validationLevel = 'error'
}: UseNodeEditorOptions): UseNodeEditorReturn => {
  // Implementation with proper error handling and type safety
  // Max 150 lines, extract complex logic to separate functions
};

// ❌ BAD: Unclear hook interface
export const useNode = (id: string) => {
  // Returns unclear structure, hard to use correctly
};
```

## State Management Standards

### Zustand Store Architecture
```typescript
// ✅ GOOD: Well-structured Zustand store
interface MindmapStore {
  // State
  nodes: Record<string, MindmapNode>;
  edges: Record<string, MindmapEdge>;
  selectedNodes: string[];
  clipboard: ClipboardData | null;
  isLoading: boolean;
  error: Error | null;
  
  // Actions (grouped by domain)
  nodeActions: {
    addNode: (node: Omit<MindmapNode, 'id'>) => string;
    updateNode: (nodeId: string, updates: Partial<MindmapNode>) => void;
    deleteNode: (nodeId: string) => void;
    duplicateNode: (nodeId: string) => string;
    moveNode: (nodeId: string, position: Position) => void;
  };
  
  selectionActions: {
    selectNode: (nodeId: string) => void;
    selectMultipleNodes: (nodeIds: string[]) => void;
    clearSelection: () => void;
    selectAll: () => void;
  };
  
  clipboardActions: {
    copyNodes: (nodeIds: string[]) => void;
    pasteNodes: (position?: Position) => string[];
    cutNodes: (nodeIds: string[]) => void;
  };
  
  // Computed values
  getSelectedNodes: () => MindmapNode[];
  getConnectedNodes: (nodeId: string) => MindmapNode[];
  validateMindmap: () => ValidationResult;
}

export const useMindmapStore = create<MindmapStore>((set, get) => ({
  // Implementation with immutable updates and proper error handling
}));
```

### State Update Patterns
```typescript
// ✅ GOOD: Immutable updates with Immer
import { produce } from 'immer';

const updateNode = (nodeId: string, updates: Partial<MindmapNode>) => {
  set(produce((state: MindmapStore) => {
    const node = state.nodes[nodeId];
    if (!node) {
      throw new NodeNotFoundError(nodeId);
    }
    
    Object.assign(node, updates);
    
    // Update derived state
    state.lastModified = new Date().toISOString();
    state.isDirty = true;
  }));
};

// ❌ BAD: Direct mutation
const updateNodeBad = (nodeId: string, updates: any) => {
  set((state) => {
    state.nodes[nodeId] = { ...state.nodes[nodeId], ...updates }; // Mutation!
    return state;
  });
};
```

## Error Handling Standards

### Comprehensive Error Types
```typescript
// ✅ GOOD: Specific error types with context
abstract class RecapMapError extends Error {
  abstract readonly code: string;
  abstract readonly category: 'user' | 'system' | 'network' | 'validation';
  
  constructor(
    message: string,
    public readonly context?: Record<string, unknown>
  ) {
    super(message);
    this.name = this.constructor.name;
  }
}

class NodeValidationError extends RecapMapError {
  readonly code = 'NODE_VALIDATION_ERROR';
  readonly category = 'validation';
  
  constructor(
    public readonly nodeId: string,
    public readonly validationErrors: ValidationError[],
    context?: Record<string, unknown>
  ) {
    super(
      `Node ${nodeId} failed validation: ${validationErrors.map(e => e.message).join(', ')}`,
      context
    );
  }
}

class APIConnectionError extends RecapMapError {
  readonly code = 'API_CONNECTION_ERROR';
  readonly category = 'network';
  
  constructor(
    public readonly endpoint: string,
    public readonly originalError: Error,
    context?: Record<string, unknown>
  ) {
    super(`Failed to connect to ${endpoint}: ${originalError.message}`, context);
  }
}
```

### Result Pattern Implementation
```typescript
// ✅ GOOD: Result pattern for error handling
type Result<T, E extends Error> = 
  | { success: true; data: T }
  | { success: false; error: E };

const saveNode = async (node: MindmapNode): Promise<Result<void, SaveError>> => {
  try {
    const validation = validateNode(node);
    if (!validation.isValid) {
      return {
        success: false,
        error: new NodeValidationError(node.id, validation.errors)
      };
    }
    
    await apiClient.saveNode(node);
    return { success: true, data: undefined };
    
  } catch (error) {
    if (error instanceof NetworkError) {
      return {
        success: false,
        error: new APIConnectionError('/nodes', error)
      };
    }
    
    throw error; // Re-throw unexpected errors
  }
};

// Usage with proper error handling
const handleSaveNode = async (node: MindmapNode) => {
  const result = await saveNode(node);
  
  if (!result.success) {
    if (result.error instanceof NodeValidationError) {
      showValidationErrors(result.error.validationErrors);
    } else if (result.error instanceof APIConnectionError) {
      showNetworkError(result.error.message);
    }
    return;
  }
  
  showSuccessMessage('Node saved successfully');
};
```

### User-Friendly Error Messages
```typescript
// ✅ GOOD: Error message mapping for users
const ERROR_MESSAGES: Record<string, (error: RecapMapError) => string> = {
  NODE_VALIDATION_ERROR: (error: NodeValidationError) => 
    `Please fix the following issues with "${error.nodeId}": ${error.validationErrors.map(e => e.userMessage).join(', ')}`,
    
  API_CONNECTION_ERROR: (error: APIConnectionError) =>
    'Unable to save your changes right now. Please check your internet connection and try again.',
    
  NODE_NOT_FOUND_ERROR: (error: NodeNotFoundError) =>
    'The requested item could not be found. It may have been deleted by another user.',
};

const getUserFriendlyMessage = (error: RecapMapError): string => {
  const messageGenerator = ERROR_MESSAGES[error.code];
  return messageGenerator ? messageGenerator(error) : 'An unexpected error occurred. Please try again.';
};
```

## Testing Standards

### Component Testing Patterns
```typescript
// ✅ GOOD: Comprehensive component tests
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import { NodeEditor } from './NodeEditor';
import { createMockNode } from '../../test-utils/mockData';

describe('NodeEditor', () => {
  const mockOnSave = vi.fn();
  const mockOnCancel = vi.fn();
  const mockNode = createMockNode({ type: NodeType.USE_CASE });
  
  beforeEach(() => {
    vi.clearAllMocks();
  });
  
  it('displays node properties correctly', () => {
    render(
      <NodeEditor
        node={mockNode}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );
    
    expect(screen.getByDisplayValue(mockNode.data.title)).toBeInTheDocument();
    expect(screen.getByText(mockNode.type)).toBeInTheDocument();
  });
  
  it('validates input and shows error messages', async () => {
    render(<NodeEditor node={mockNode} onSave={mockOnSave} onCancel={mockOnCancel} />);
    
    const titleInput = screen.getByLabelText('Title');
    fireEvent.change(titleInput, { target: { value: '' } });
    fireEvent.blur(titleInput);
    
    await waitFor(() => {
      expect(screen.getByText('Title is required')).toBeInTheDocument();
    });
    
    expect(mockOnSave).not.toHaveBeenCalled();
  });
  
  it('calls onSave with updated node data', async () => {
    render(<NodeEditor node={mockNode} onSave={mockOnSave} onCancel={mockOnCancel} />);
    
    const titleInput = screen.getByLabelText('Title');
    fireEvent.change(titleInput, { target: { value: 'Updated Title' } });
    
    const saveButton = screen.getByRole('button', { name: 'Save' });
    fireEvent.click(saveButton);
    
    await waitFor(() => {
      expect(mockOnSave).toHaveBeenCalledWith({
        ...mockNode,
        data: { ...mockNode.data, title: 'Updated Title' }
      });
    });
  });
  
  it('handles loading and error states', async () => {
    const { rerender } = render(
      <NodeEditor node={mockNode} onSave={mockOnSave} onCancel={mockOnCancel} />
    );
    
    // Test loading state
    rerender(
      <NodeEditor 
        node={mockNode} 
        onSave={mockOnSave} 
        onCancel={mockOnCancel}
        isLoading={true}
      />
    );
    
    expect(screen.getByRole('button', { name: 'Saving...' })).toBeDisabled();
    
    // Test error state
    const error = new NodeValidationError('test-id', []);
    rerender(
      <NodeEditor 
        node={mockNode} 
        onSave={mockOnSave} 
        onCancel={mockOnCancel}
        error={error}
      />
    );
    
    expect(screen.getByText(/validation error/i)).toBeInTheDocument();
  });
});
```

### Custom Hooks Testing
```typescript
// ✅ GOOD: Hook testing with proper setup
import { renderHook, act, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import { useNodeEditor } from './useNodeEditor';
import { createMockNode } from '../../test-utils/mockData';

// Mock the API service
vi.mock('../../services/apiClient', () => ({
  apiClient: {
    saveNode: vi.fn(),
    deleteNode: vi.fn(),
  }
}));

describe('useNodeEditor', () => {
  const mockNode = createMockNode();
  
  beforeEach(() => {
    vi.clearAllMocks();
  });
  
  it('loads node data and manages state correctly', async () => {
    const { result } = renderHook(() => 
      useNodeEditor({ nodeId: mockNode.id })
    );
    
    expect(result.current.isLoading).toBe(true);
    
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.node).toEqual(mockNode);
    });
  });
  
  it('tracks dirty state when properties change', () => {
    const { result } = renderHook(() => 
      useNodeEditor({ nodeId: mockNode.id })
    );
    
    expect(result.current.isDirty).toBe(false);
    
    act(() => {
      result.current.updateProperty('title', 'New Title');
    });
    
    expect(result.current.isDirty).toBe(true);
  });
  
  it('validates node properties and returns errors', () => {
    const { result } = renderHook(() => 
      useNodeEditor({ nodeId: mockNode.id })
    );
    
    act(() => {
      result.current.updateProperty('title', ''); // Invalid empty title
    });
    
    const validation = result.current.validateNode();
    expect(validation.isValid).toBe(false);
    expect(validation.errors).toHaveLength(1);
    expect(validation.errors[0].field).toBe('title');
  });
});
```

## Performance Standards

### React Performance Optimization
```typescript
// ✅ GOOD: Optimized component with proper memoization
import React, { memo, useMemo, useCallback } from 'react';

interface NodeListProps {
  nodes: MindmapNode[];
  selectedNodeIds: Set<string>;
  onNodeSelect: (nodeId: string) => void;
  onNodeEdit: (nodeId: string) => void;
}

export const NodeList = memo<NodeListProps>(({
  nodes,
  selectedNodeIds,
  onNodeSelect,
  onNodeEdit
}) => {
  // Memoize expensive computations
  const sortedNodes = useMemo(() => 
    [...nodes].sort((a, b) => a.data.title.localeCompare(b.data.title)),
    [nodes]
  );
  
  // Memoize callbacks to prevent unnecessary re-renders
  const handleNodeClick = useCallback((nodeId: string) => {
    onNodeSelect(nodeId);
  }, [onNodeSelect]);
  
  const handleNodeDoubleClick = useCallback((nodeId: string) => {
    onNodeEdit(nodeId);
  }, [onNodeEdit]);
  
  return (
    <div className="node-list">
      {sortedNodes.map(node => (
        <NodeListItem
          key={node.id}
          node={node}
          isSelected={selectedNodeIds.has(node.id)}
          onClick={handleNodeClick}
          onDoubleClick={handleNodeDoubleClick}
        />
      ))}
    </div>
  );
});

NodeList.displayName = 'NodeList';
```

### Bundle Size Optimization
```typescript
// ✅ GOOD: Lazy loading and code splitting
import { lazy, Suspense } from 'react';
import { LoadingSpinner } from '../atoms/LoadingSpinner';

// Lazy load heavy components
const NodeEditor = lazy(() => import('../organisms/NodeEditor'));
const ChatPanel = lazy(() => import('../organisms/ChatPanel'));
const YAMLExporter = lazy(() => import('../organisms/YAMLExporter'));

// Use dynamic imports for large libraries
const importChartLibrary = () => import('chart.js').then(module => module.Chart);

export const DashboardLayout = () => {
  return (
    <div className="dashboard-layout">
      <Suspense fallback={<LoadingSpinner />}>
        <NodeEditor />
      </Suspense>
      
      <Suspense fallback={<LoadingSpinner />}>
        <ChatPanel />
      </Suspense>
    </div>
  );
};
```

## Code Review Standards

### Review Checklist
```yaml
mandatory_checks:
  type_safety:
    - "No 'any' types without explicit justification"
    - "All function parameters and return types defined"
    - "Proper error type definitions and handling"
    - "Union types used instead of magic strings"
  
  component_quality:
    - "Component size within limits for its type"
    - "Props interface clearly defined and documented"
    - "Proper accessibility attributes (ARIA, semantic HTML)"
    - "Loading and error states handled"
  
  testing:
    - "New functionality has corresponding tests"
    - "Edge cases and error scenarios covered"
    - "Test names clearly describe what is being tested"
    - "Mock usage is appropriate and minimal"
  
  performance:
    - "Unnecessary re-renders prevented (memo, useCallback)"
    - "Large components are lazy-loaded"
    - "Expensive computations are memoized"
    - "Bundle impact considered for new dependencies"
  
  security:
    - "User input is validated and sanitized"
    - "XSS prevention measures in place"
    - "Sensitive data not logged or exposed"
    - "Proper authentication checks for protected operations"
```

### Code Review Process
```yaml
review_workflow:
  author_responsibilities:
    - "Self-review code before requesting review"
    - "Run all tests and linting locally"
    - "Provide clear PR description with context"
    - "Link to relevant issues or documentation"
    - "Test on multiple browsers if UI changes"
  
  reviewer_responsibilities:
    - "Review within 24 hours during business days"
    - "Check code against standards checklist"
    - "Test functionality locally for significant changes"
    - "Provide constructive, specific feedback"
    - "Approve only when confident in code quality"
  
  approval_requirements:
    small_changes: "1 approval from team member"
    medium_changes: "1 approval from senior developer"
    large_changes: "2 approvals including team lead"
    architecture_changes: "Architecture review meeting required"
```

## Documentation Standards

### Code Documentation
```typescript
// ✅ GOOD: Comprehensive JSDoc documentation
/**
 * Validates a mindmap node according to its type-specific rules
 * 
 * @param node - The node to validate
 * @param context - Additional context for validation (optional)
 * @returns Validation result with details about any errors
 * 
 * @example
 * ```typescript
 * const node = createUseCaseNode({ title: 'User Login' });
 * const result = validateNode(node);
 * 
 * if (!result.isValid) {
 *   console.log('Validation errors:', result.errors);
 * }
 * ```
 * 
 * @throws {NodeValidationError} When node structure is fundamentally invalid
 * @since 1.0.0
 */
export const validateNode = (
  node: MindmapNode,
  context?: ValidationContext
): ValidationResult => {
  // Implementation
};
```

### README Standards for Components
```markdown
# ComponentName

Brief description of what this component does and when to use it.

## Usage

```typescript
import { ComponentName } from './ComponentName';

<ComponentName
  prop1="value"
  prop2={complexValue}
  onAction={handleAction}
/>
```

## Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| prop1 | string | Yes | - | Description of prop1 |
| prop2 | ComplexType | No | defaultValue | Description of prop2 |

## Examples

### Basic Usage
[Example code]

### Advanced Usage
[Example code with complex scenarios]

## Accessibility

- Supports keyboard navigation
- Screen reader compatible
- Follows WCAG 2.1 AA guidelines

## Testing

Run component tests: `npm test ComponentName`

## Related Components

- [RelatedComponent1](../RelatedComponent1)
- [RelatedComponent2](../RelatedComponent2)
```

## Deployment and Operations Standards

### Environment Configuration
```typescript
// ✅ GOOD: Type-safe environment configuration
interface EnvironmentConfig {
  NODE_ENV: 'development' | 'staging' | 'production';
  API_BASE_URL: string;
  WEBSOCKET_URL: string;
  FEATURE_FLAGS: {
    enableChat: boolean;
    enableRealTimeCollab: boolean;
    enableAdvancedExport: boolean;
  };
  MONITORING: {
    sentryDsn?: string;
    logLevel: 'debug' | 'info' | 'warn' | 'error';
  };
}

const createConfig = (): EnvironmentConfig => {
  const config = {
    NODE_ENV: process.env.NODE_ENV as EnvironmentConfig['NODE_ENV'],
    API_BASE_URL: process.env.VITE_API_BASE_URL,
    WEBSOCKET_URL: process.env.VITE_WEBSOCKET_URL,
    FEATURE_FLAGS: {
      enableChat: process.env.VITE_ENABLE_CHAT === 'true',
      enableRealTimeCollab: process.env.VITE_ENABLE_REALTIME === 'true',
      enableAdvancedExport: process.env.VITE_ENABLE_ADVANCED_EXPORT === 'true',
    },
    MONITORING: {
      sentryDsn: process.env.VITE_SENTRY_DSN,
      logLevel: (process.env.VITE_LOG_LEVEL as any) || 'info',
    },
  };
  
  // Validate required configuration
  if (!config.API_BASE_URL) {
    throw new Error('VITE_API_BASE_URL is required');
  }
  
  return config;
};

export const config = createConfig();
```

### Logging Standards
```typescript
// ✅ GOOD: Structured logging with context
interface LogContext {
  userId?: string;
  sessionId?: string;
  nodeId?: string;
  operation?: string;
  [key: string]: unknown;
}

class Logger {
  private context: LogContext = {};
  
  setContext(context: Partial<LogContext>) {
    this.context = { ...this.context, ...context };
  }
  
  info(message: string, additionalContext?: LogContext) {
    this.log('info', message, additionalContext);
  }
  
  warn(message: string, additionalContext?: LogContext) {
    this.log('warn', message, additionalContext);
  }
  
  error(message: string, error?: Error, additionalContext?: LogContext) {
    this.log('error', message, { 
      ...additionalContext, 
      error: error?.message,
      stack: error?.stack 
    });
  }
  
  private log(level: string, message: string, additionalContext?: LogContext) {
    const logEntry = {
      timestamp: new Date().toISOString(),
      level,
      message,
      context: { ...this.context, ...additionalContext }
    };
    
    if (config.NODE_ENV === 'development') {
      console.log(JSON.stringify(logEntry, null, 2));
    } else {
      // Send to logging service
      this.sendToLoggingService(logEntry);
    }
  }
  
  private sendToLoggingService(logEntry: any) {
    // Implementation for production logging
  }
}

export const logger = new Logger();

// Usage example
logger.setContext({ userId: 'user123', sessionId: 'session456' });
logger.info('Node created', { nodeId: 'node789', operation: 'create_use_case' });
```

## Security Standards

### Input Validation
```typescript
// ✅ GOOD: Comprehensive input validation with Zod
import { z } from 'zod';

const NodeDataSchema = z.object({
  title: z.string()
    .min(1, 'Title is required')
    .max(100, 'Title must be less than 100 characters')
    .regex(/^[a-zA-Z0-9\s\-_.]+$/, 'Title contains invalid characters'),
    
  description: z.string()
    .max(1000, 'Description must be less than 1000 characters')
    .optional(),
    
  properties: z.record(z.unknown())
    .refine(
      (props) => Object.keys(props).length <= 50,
      'Too many properties'
    ),
    
  tags: z.array(z.string())
    .max(10, 'Too many tags')
    .optional()
});

export const validateNodeData = (data: unknown): NodeData => {
  try {
    return NodeDataSchema.parse(data);
  } catch (error) {
    if (error instanceof z.ZodError) {
      throw new NodeValidationError('validation_failed', error.errors);
    }
    throw error;
  }
};

// XSS Prevention
export const sanitizeHTML = (input: string): string => {
  // Use DOMPurify for HTML sanitization
  return DOMPurify.sanitize(input, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'p', 'br'],
    ALLOWED_ATTR: []
  });
};
```

### Authentication Security
```typescript
// ✅ GOOD: Secure token handling
class TokenManager {
  private static readonly ACCESS_TOKEN_KEY = 'recap_access_token';
  private static readonly REFRESH_TOKEN_KEY = 'recap_refresh_token';
  
  static setTokens(accessToken: string, refreshToken: string) {
    // Store access token in memory only
    this.accessToken = accessToken;
    
    // Store refresh token in httpOnly cookie (handled by backend)
    // Never store refresh token in localStorage
  }
  
  static getAccessToken(): string | null {
    return this.accessToken;
  }
  
  static clearTokens() {
    this.accessToken = null;
    // Clear httpOnly cookie through API call
    apiClient.logout();
  }
  
  static isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
  
  private static accessToken: string | null = null;
}
```

## Continuous Improvement

### Code Quality Metrics
```yaml
quality_metrics:
  automated_tracking:
    test_coverage: "Target: >90% for critical paths"
    type_safety: "Target: 0 'any' types in production"
    bundle_size: "Target: <500KB initial bundle"
    performance: "Target: <2s load time, <100ms interactions"
    
  code_review_metrics:
    review_time: "Target: <24 hours"
    approval_rate: "Target: >80% approved without major changes"
    defect_rate: "Target: <5% of PRs need follow-up fixes"
    
  technical_debt:
    todo_items: "Must have issue numbers and be tracked"
    deprecated_usage: "Regular audits and migration plans"
    dependency_updates: "Monthly security updates, quarterly feature updates"
```

### Refactoring Guidelines
```yaml
refactoring_triggers:
  component_size: "When approaching 80% of size limits"
  code_duplication: "When same pattern used 3+ times"
  performance_degradation: "When metrics fall below targets"
  complexity_increase: "When cognitive complexity gets high"
  
refactoring_approach:
  plan_first: "Create issue with refactoring plan before starting"
  incremental: "Small, reviewable changes over large rewrites"
  test_coverage: "Ensure tests pass before and after refactoring"
  backwards_compatibility: "Maintain API compatibility when possible"
  documentation_update: "Update docs and examples after refactoring"
```

## Enforcement and Tooling

### Automated Enforcement
```json
// .eslintrc.json - Custom rules for RecapMap standards
{
  "extends": [
    "@typescript-eslint/recommended",
    "plugin:react/recommended",
    "plugin:react-hooks/recommended"
  ],
  "rules": {
    "@typescript-eslint/no-explicit-any": "error",
    "@typescript-eslint/no-unused-vars": "error",
    "max-lines": ["error", { "max": 200 }],
    "max-params": ["error", 8],
    "complexity": ["error", 10],
    "no-console": "error",
    "no-debugger": "error",
    "prefer-const": "error",
    "react-hooks/exhaustive-deps": "error"
  },
  "overrides": [
    {
      "files": ["src/components/atoms/**/*.tsx"],
      "rules": {
        "max-lines": ["error", { "max": 50 }]
      }
    },
    {
      "files": ["src/components/molecules/**/*.tsx"],
      "rules": {
        "max-lines": ["error", { "max": 100 }]
      }
    }
  ]
}
```

### Pre-commit Hooks
```yaml
# .pre-commit-config.yaml
repos:
  - repo: local
    hooks:
      - id: typescript-check
        name: TypeScript Check
        entry: npx tsc --noEmit
        language: system
        
      - id: eslint
        name: ESLint
        entry: npx eslint --fix
        language: system
        files: \.(ts|tsx)$
        
      - id: prettier
        name: Prettier
        entry: npx prettier --write
        language: system
        files: \.(ts|tsx|json|md)$
        
      - id: test
        name: Run Tests
        entry: npm test
        language: system
        pass_filenames: false
```

## Conclusion

These coding discipline and standards ensure that RecapMap develops into a maintainable, scalable, and high-quality platform. By enforcing these standards from the beginning, we:

1. **Prevent Technical Debt**: Strict standards prevent shortcuts that create future problems
2. **Enable Team Scaling**: Clear patterns make it easy for new developers to contribute
3. **Ensure Production Readiness**: Enterprise-grade quality from day one
4. **Maintain Performance**: Optimization built into development process
5. **Support Long-term Vision**: Architecture that can grow with our ambitious goals

Adherence to these standards is not optional - they are the foundation that will enable RecapMap to achieve its vision of revolutionizing visual business logic design.

---

*"Quality is not an act, it is a habit" - Aristotle*
