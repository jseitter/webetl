package io.webetl.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.Map;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIResponse {
    private final String content;
    private Map<String, Object> flowSuggestion;

    public AIResponse(String content) {
        this.content = content;
    }

    public AIResponse(String content, Map<String, Object> flowSuggestion) {
        this.content = content;
        this.flowSuggestion = flowSuggestion;
    }

    public String getContent() {
        return content;
    }
} 