# AI Integration Architecture - Custom Orchestration System

**Date**: June 1, 2025  
**Project**: RecapMap Java Backend  
**Focus**: Custom AI Orchestration (No LangChain)

## AI Orchestration Philosophy

### Why Build Our Own?
```yaml
rationale: "Custom control over AI interactions"
benefits:
  - "Direct API control (cost optimization)"
  - "Custom memory management"
  - "Multi-provider support"
  - "Conversation flow control"
  - "Business logic integration"
  - "Enhanced monitoring and debugging"

vs_langchain:
  - "No external framework dependencies"
  - "Tailored for RecapMap's specific needs"
  - "Better performance (fewer abstractions)"
  - "Easier to debug and maintain"
```

## Core AI Architecture

### 1. AI Orchestration Service
```java
@Service
public class AIOrchestrationService {
    
    private final Map<String, AIProvider> providers;
    private final AIMemoryService memoryService;
    private final AIConversationService conversationService;
    private final AIPromptService promptService;
    
    @Autowired
    public AIOrchestrationService(
            List<AIProvider> providers,
            AIMemoryService memoryService,
            AIConversationService conversationService,
            AIPromptService promptService) {
        
        this.providers = providers.stream()
            .collect(Collectors.toMap(AIProvider::getName, Function.identity()));
        this.memoryService = memoryService;
        this.conversationService = conversationService;
        this.promptService = promptService;
    }
    
    public CompletableFuture<AIResponse> processMessage(AIRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            
            // 1. Load conversation context
            AIConversation conversation = conversationService
                .getOrCreateConversation(request.getConversationId());
            
            // 2. Build enhanced prompt with memory
            String enhancedPrompt = promptService.buildPrompt(
                request.getMessage(),
                conversation,
                request.getContextType()
            );
            
            // 3. Select appropriate AI provider
            AIProvider provider = selectProvider(request.getProviderPreference());
            
            // 4. Make AI call with retry logic
            AIResponse response = makeAICall(provider, enhancedPrompt, request);
            
            // 5. Update conversation and memory
            conversationService.addMessage(conversation.getId(), request.getMessage(), response);
            memoryService.updateMemory(conversation.getId(), response);
            
            // 6. Process and enhance response
            return processResponse(response, request.getContextType());
        });
    }
    
    private AIProvider selectProvider(String preference) {
        // Provider selection logic based on:
        // - User preference
        // - Request type (mindmap vs code generation)
        // - Current availability and performance
        // - Cost considerations
        
        if (preference != null && providers.containsKey(preference)) {
            return providers.get(preference);
        }
        
        // Default selection logic
        return providers.get("openai"); // Fallback to OpenAI
    }
    
    private AIResponse makeAICall(AIProvider provider, String prompt, AIRequest request) {
        try {
            return provider.sendMessage(prompt, request.getOptions());
        } catch (AIProviderException e) {
            // Retry with different provider or handle gracefully
            log.warn("AI call failed with {}, trying fallback", provider.getName(), e);
            return makeFallbackCall(prompt, request);
        }
    }
}
```

### 2. AI Provider Abstraction
```java
public interface AIProvider {
    String getName();
    AIResponse sendMessage(String prompt, AIRequestOptions options);
    CompletableFuture<AIResponse> sendMessageAsync(String prompt, AIRequestOptions options);
    boolean isAvailable();
    ProviderCapabilities getCapabilities();
}

@Component
public class OpenAIProvider implements AIProvider {
    
    @Value("${ai.openai.api-key}")
    private String apiKey;
    
    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public AIResponse sendMessage(String prompt, AIRequestOptions options) {
        
        OpenAIRequest request = OpenAIRequest.builder()
            .model(options.getModel() != null ? options.getModel() : "gpt-4")
            .messages(List.of(new OpenAIMessage("user", prompt)))
            .maxTokens(options.getMaxTokens())
            .temperature(options.getTemperature())
            .build();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<OpenAIRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<OpenAIResponse> response = restTemplate.postForEntity(
                baseUrl + "/chat/completions",
                entity,
                OpenAIResponse.class
            );
            
            return convertToAIResponse(response.getBody());
            
        } catch (Exception e) {
            throw new AIProviderException("OpenAI call failed", e);
        }
    }
    
    @Override
    public CompletableFuture<AIResponse> sendMessageAsync(String prompt, AIRequestOptions options) {
        return CompletableFuture.supplyAsync(() -> sendMessage(prompt, options));
    }
    
    private AIResponse convertToAIResponse(OpenAIResponse response) {
        return AIResponse.builder()
            .content(response.getChoices().get(0).getMessage().getContent())
            .model(response.getModel())
            .tokenUsage(response.getUsage())
            .provider("openai")
            .timestamp(LocalDateTime.now())
            .build();
    }
}

@Component
public class ClaudeProvider implements AIProvider {
    
    @Value("${ai.anthropic.api-key}")
    private String apiKey;
    
    // Similar implementation for Claude API
    
    @Override
    public AIResponse sendMessage(String prompt, AIRequestOptions options) {
        // Claude-specific implementation
        ClaudeRequest request = ClaudeRequest.builder()
            .model(options.getModel() != null ? options.getModel() : "claude-3-sonnet-20240229")
            .maxTokens(options.getMaxTokens())
            .messages(List.of(new ClaudeMessage("user", prompt)))
            .build();
        
        // Make API call to Claude
        // ... implementation
    }
}
```

### 3. Memory Management System
```java
@Service
public class AIMemoryService {
    
    private final AIMemoryRepository memoryRepository;
    private final EmbeddingService embeddingService; // For semantic search
    
    public void updateMemory(String conversationId, AIResponse response) {
        
        // Extract key information from AI response
        MemoryExtraction extraction = extractMemoryFromResponse(response);
        
        // Store conversation summary
        if (extraction.hasImportantInformation()) {
            AIMemoryEntry entry = AIMemoryEntry.builder()
                .conversationId(conversationId)
                .content(extraction.getSummary())
                .concepts(extraction.getConcepts())
                .entities(extraction.getEntities())
                .importance(extraction.getImportance())
                .timestamp(LocalDateTime.now())
                .build();
            
            memoryRepository.save(entry);
        }
        
        // Update long-term memory with embeddings for semantic search
        if (extraction.isSignificant()) {
            embeddingService.storeEmbedding(conversationId, extraction.getContent());
        }
    }
    
    public List<AIMemoryEntry> getRelevantMemory(String query, String contextType) {
        
        // 1. Get recent memories (last 10 interactions)
        List<AIMemoryEntry> recentMemories = memoryRepository
            .findTopByOrderByTimestampDesc(10);
        
        // 2. Get semantically similar memories
        List<AIMemoryEntry> similarMemories = embeddingService
            .findSimilarMemories(query, 5);
        
        // 3. Combine and rank by relevance
        return combineAndRankMemories(recentMemories, similarMemories, contextType);
    }
    
    private MemoryExtraction extractMemoryFromResponse(AIResponse response) {
        // Extract important information using:
        // - Named entity recognition
        // - Key concept extraction
        // - Decision point identification
        // - Action item extraction
        
        return MemoryExtraction.builder()
            .summary(summarizeResponse(response))
            .concepts(extractConcepts(response))
            .entities(extractEntities(response))
            .importance(calculateImportance(response))
            .build();
    }
}

@Document(collection = "ai_memory")
public class AIMemoryEntry {
    @Id
    private String id;
    
    private String conversationId;
    private String content;
    private List<String> concepts;
    private Map<String, String> entities;
    private ImportanceLevel importance;
    private LocalDateTime timestamp;
    
    // Embedding for semantic search
    private List<Double> embedding;
    
    public enum ImportanceLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
```

### 4. Prompt Engineering Service
```java
@Service
public class AIPromptService {
    
    private final Map<String, PromptTemplate> templates;
    
    @PostConstruct
    public void initializeTemplates() {
        templates.put("mindmap_analysis", loadTemplate("mindmap_analysis.md"));
        templates.put("yaml_generation", loadTemplate("yaml_generation.md"));
        templates.put("node_suggestion", loadTemplate("node_suggestion.md"));
        templates.put("validation_feedback", loadTemplate("validation_feedback.md"));
    }
    
    public String buildPrompt(String userMessage, AIConversation conversation, String contextType) {
        
        // 1. Get relevant memory context
        List<AIMemoryEntry> relevantMemory = memoryService.getRelevantMemory(userMessage, contextType);
        
        // 2. Get conversation history (last N messages)
        List<AIMessage> recentMessages = conversation.getMessages()
            .stream()
            .sorted(Comparator.comparing(AIMessage::getTimestamp).reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        // 3. Build context-specific prompt
        PromptBuilder builder = PromptBuilder.create()
            .withTemplate(getTemplate(contextType))
            .withSystemContext(buildSystemContext(contextType))
            .withMemoryContext(relevantMemory)
            .withConversationHistory(recentMessages)
            .withUserMessage(userMessage);
        
        // 4. Add specific context based on type
        switch (contextType) {
            case "mindmap_node":
                builder.withNodeContext(getCurrentNodeContext());
                break;
            case "yaml_export":
                builder.withProjectContext(getCurrentProjectContext());
                break;
            case "validation":
                builder.withValidationContext(getCurrentValidationContext());
                break;
        }
        
        return builder.build();
    }
    
    private String buildSystemContext(String contextType) {
        return switch (contextType) {
            case "mindmap_node" -> """
                You are an expert business analyst helping to design mindmap nodes for RecapMap.
                RecapMap is a visual AGI orchestration platform that converts business logic into deployable applications.
                
                Your role:
                - Help users refine node descriptions and properties
                - Suggest connections between nodes
                - Validate business logic consistency
                - Provide implementation guidance
                
                Focus on business value and clarity, not technical implementation details.
                """;
                
            case "yaml_export" -> """
                You are an expert in converting visual business logic into structured YAML specifications.
                Your role is to help generate clean, comprehensive YAML that AI agents can use for code generation.
                
                Guidelines:
                - Maintain business focus (avoid technical implementation)
                - Ensure all node relationships are captured
                - Include clear acceptance criteria
                - Validate logical consistency
                """;
                
            default -> "You are an AI assistant specialized in business process design and visual modeling.";
        };
    }
}

@Component
public class PromptBuilder {
    private StringBuilder prompt = new StringBuilder();
    
    public static PromptBuilder create() {
        return new PromptBuilder();
    }
    
    public PromptBuilder withTemplate(PromptTemplate template) {
        prompt.append(template.getContent()).append("\n\n");
        return this;
    }
    
    public PromptBuilder withSystemContext(String context) {
        prompt.append("## System Context\n")
              .append(context)
              .append("\n\n");
        return this;
    }
    
    public PromptBuilder withMemoryContext(List<AIMemoryEntry> memories) {
        if (!memories.isEmpty()) {
            prompt.append("## Relevant Context from Previous Conversations\n");
            memories.forEach(memory -> 
                prompt.append("- ").append(memory.getContent()).append("\n")
            );
            prompt.append("\n");
        }
        return this;
    }
    
    public PromptBuilder withUserMessage(String message) {
        prompt.append("## User Request\n")
              .append(message)
              .append("\n\n");
        return this;
    }
    
    public String build() {
        return prompt.toString();
    }
}
```

### 5. Multi-Agent Conversation System
```java
@Service
public class MultiAgentConversationService {
    
    private final Map<String, AIAgent> agents;
    
    @PostConstruct
    public void initializeAgents() {
        agents.put("business_analyst", new BusinessAnalystAgent());
        agents.put("technical_architect", new TechnicalArchitectAgent());
        agents.put("validation_expert", new ValidationExpertAgent());
        agents.put("export_specialist", new ExportSpecialistAgent());
    }
    
    public CompletableFuture<MultiAgentResponse> processWithMultipleAgents(
            String userMessage, 
            String contextType,
            List<String> requiredAgents) {
        
        // 1. Prepare shared context
        SharedContext context = buildSharedContext(userMessage, contextType);
        
        // 2. Parallel processing with different agents
        List<CompletableFuture<AgentResponse>> agentTasks = requiredAgents.stream()
            .map(agentName -> agents.get(agentName))
            .map(agent -> agent.processAsync(context))
            .collect(Collectors.toList());
        
        // 3. Combine results
        return CompletableFuture.allOf(agentTasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<AgentResponse> responses = agentTasks.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                
                return synthesizeResponses(responses, context);
            });
    }
    
    private MultiAgentResponse synthesizeResponses(List<AgentResponse> responses, SharedContext context) {
        // Combine different agent perspectives into coherent response
        // Handle conflicts and contradictions
        // Prioritize responses based on context
        
        return MultiAgentResponse.builder()
            .primaryResponse(selectPrimaryResponse(responses, context))
            .alternativeViews(extractAlternativeViews(responses))
            .consensus(findConsensus(responses))
            .conflictResolution(resolveConflicts(responses))
            .build();
    }
}

public interface AIAgent {
    String getName();
    AgentResponse process(SharedContext context);
    CompletableFuture<AgentResponse> processAsync(SharedContext context);
}

@Component
public class BusinessAnalystAgent implements AIAgent {
    
    private final AIOrchestrationService orchestrationService;
    
    @Override
    public AgentResponse process(SharedContext context) {
        
        String specializedPrompt = buildBusinessAnalystPrompt(context);
        
        AIRequest request = AIRequest.builder()
            .message(specializedPrompt)
            .contextType("business_analysis")
            .providerPreference("openai")
            .options(AIRequestOptions.builder()
                .model("gpt-4")
                .temperature(0.7)
                .maxTokens(1000)
                .build())
            .build();
        
        AIResponse response = orchestrationService.processMessage(request).join();
        
        return AgentResponse.builder()
            .agentName("business_analyst")
            .content(response.getContent())
            .confidence(calculateConfidence(response, context))
            .recommendations(extractRecommendations(response))
            .build();
    }
    
    private String buildBusinessAnalystPrompt(SharedContext context) {
        return """
            As a senior business analyst, analyze the following mindmap design from a business perspective:
            
            Context: %s
            
            Focus on:
            - Business value and ROI
            - Process efficiency
            - Stakeholder impact
            - Risk assessment
            - Implementation feasibility
            
            Provide specific, actionable recommendations.
            """.formatted(context.getDescription());
    }
}
```

### 6. AI Response Processing
```java
@Service
public class AIResponseProcessor {
    
    public ProcessedAIResponse processResponse(AIResponse rawResponse, String contextType) {
        
        return switch (contextType) {
            case "mindmap_node" -> processNodeSuggestion(rawResponse);
            case "yaml_export" -> processYamlGeneration(rawResponse);
            case "validation" -> processValidationFeedback(rawResponse);
            default -> processGenericResponse(rawResponse);
        };
    }
    
    private ProcessedAIResponse processNodeSuggestion(AIResponse response) {
        
        // Extract structured information from AI response
        NodeSuggestion suggestion = parseNodeSuggestion(response.getContent());
        
        return ProcessedAIResponse.builder()
            .originalResponse(response)
            .structuredData(suggestion)
            .actionableItems(extractActionItems(response))
            .suggestedChanges(extractSuggestedChanges(response))
            .build();
    }
    
    private NodeSuggestion parseNodeSuggestion(String content) {
        // Use regex or NLP to extract:
        // - Suggested node titles
        // - Property recommendations
        // - Connection suggestions
        // - Validation feedback
        
        return NodeSuggestion.builder()
            .suggestedTitle(extractSuggestedTitle(content))
            .propertyChanges(extractPropertyChanges(content))
            .connectionSuggestions(extractConnectionSuggestions(content))
            .validationIssues(extractValidationIssues(content))
            .build();
    }
}
```

## Configuration and Monitoring

### AI Configuration
```yaml
# application.yml
ai:
  providers:
    openai:
      enabled: true
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      default-model: gpt-4
      max-tokens: 2000
      temperature: 0.7
      timeout: 30s
    
    anthropic:
      enabled: true
      api-key: ${ANTHROPIC_API_KEY}
      base-url: https://api.anthropic.com
      default-model: claude-3-sonnet-20240229
      max-tokens: 2000
      temperature: 0.7
      timeout: 30s
  
  memory:
    max-entries-per-conversation: 100
    embedding-model: text-embedding-ada-002
    similarity-threshold: 0.8
  
  monitoring:
    log-requests: true
    log-responses: false  # Don't log sensitive data
    track-costs: true
    alert-on-failures: true
```

### Usage Monitoring
```java
@Component
public class AIUsageMonitor {
    
    @EventListener
    public void handleAIRequest(AIRequestEvent event) {
        // Track usage metrics
        // Monitor costs
        // Alert on anomalies
    }
    
    public AIUsageReport generateUsageReport(String userId, LocalDateTime from, LocalDateTime to) {
        // Generate cost and usage reports
    }
}
```

This custom AI orchestration system gives you full control over your AI interactions while providing the flexibility to integrate multiple providers and sophisticated memory management.
