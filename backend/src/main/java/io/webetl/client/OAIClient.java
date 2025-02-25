package io.webetl.client;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OAIClient {
    private final OpenAIClient client;
    private final boolean enabled;

    public OAIClient(@Value("${openai.api.key:}") String apiKey) {
        this.enabled = apiKey != null && !apiKey.trim().isEmpty();
        if (!enabled) {
            this.client = null;
            log.warn("OpenAI client disabled - no API key configured");
            return;
        }
        this.client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .build();
    }

    public AIResponse getCompletion(String prompt) {
        if (!enabled) {
            return new AIResponse("Chat service is not available - no API key configured");
        }
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_3_5_TURBO)
                .addUserMessage(prompt)
                .build();
            
            ChatCompletion completion = client.chat().completions().create(params);
            String content = completion.choices().get(0).message().content().orElse("");
            return new AIResponse(content);
        } catch (Exception e) {
            log.warn("OpenAI error: {}", e.getMessage());
            return new AIResponse("Sorry, there was an error communicating with the AI service: " + 
                e.getMessage());
        }
    }
} 