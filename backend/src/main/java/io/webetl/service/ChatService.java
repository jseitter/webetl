package io.webetl.service;

import org.springframework.stereotype.Service;
import io.webetl.client.OAIClient;
import io.webetl.client.AIResponse;
import io.webetl.model.component.ComponentCategory;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Service
public class ChatService {
    private final OAIClient openAIClient;
    private final ComponentService componentService;

    public ChatService(OAIClient openAIClient, ComponentService componentService) {
        this.openAIClient = openAIClient;
        this.componentService = componentService;
    }

    public AIResponse processChat(String message) {
        // 1. Get available components
        List<ComponentCategory> availableComponents = componentService.getAvailableComponents();
        
        // 2. Create AI prompt with component context
        String prompt = createPromptWithContext(message, availableComponents);
        
        // 3. Get AI response
        String aiResponse = openAIClient.getCompletion(prompt).getContent();
        
        // Check if response contains a flow suggestion (JSON format)
        if (aiResponse.contains("{") && aiResponse.contains("}")) {
            try {
                int start = aiResponse.indexOf("{");
                int end = aiResponse.lastIndexOf("}") + 1;
                String jsonPart = aiResponse.substring(start, end);
                
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> flowSuggestion = mapper.readValue(jsonPart, Map.class);
                
                String textContent = aiResponse.substring(0, start).trim();
                return new AIResponse(textContent, flowSuggestion);
            } catch (Exception e) {
                // If JSON parsing fails, return just the text
                return new AIResponse(aiResponse);
            }
        }
        return new AIResponse(aiResponse);
    }

    private String createPromptWithContext(String userMessage, List<ComponentCategory> categories) {
        return String.format("""
            You are an ETL flow design assistant. Available components:
            %s
            
            User question: %s
            
            Provide a helpful response explaining which components to use and how to connect them.
            If appropriate, suggest a flow definition in this JSON format:
            {
              "nodes": [
                {
                  "id": "file-source-1234",
                  "type": "source",
                  "data": {
                    "label": "CSV Source",
                    "componentData": {"id": "file-source"},
                    "handles": []
                  },
                  "width": 150
                },
                {
                  "id": "map-transform-1234",
                  "type": "transform",
                  "data": {
                    "label": "Map Transform",
                    "componentData": {"id": "map-transform"},
                    "handles": []
                  },
                  "width": 150
                }
              ],
              "edges": [
                {
                  "id": "edge-1234",
                  "source": "file-source-1234",
                  "target": "map-transform-1234",
                  "type": "default"
                }
              ]
            }
            """,
            formatComponentList(categories),
            userMessage
        );
    }

    private String formatComponentList(List<ComponentCategory> categories) {
        StringBuilder sb = new StringBuilder();
        for (ComponentCategory category : categories) {
            sb.append(category.getName()).append(":\n");
            category.getComponents().forEach(component -> 
                sb.append("- ").append(component.getClass().getSimpleName()).append("\n")
            );
        }
        return sb.toString();
    }
} 