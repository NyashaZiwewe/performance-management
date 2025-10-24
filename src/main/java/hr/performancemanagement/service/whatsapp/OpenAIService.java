package hr.performancemanagement.service.whatsapp;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class OpenAIService {

    private final WebClient webClient;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    public OpenAIService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    // Stores chat sessions per WhatsApp user
    private final Map<String, List<ChatMessage>> sessions = new HashMap<>();

    public String getChatCompletion(String userId, String userMessage) {

        // Retrieve or create chat history for the user
        List<ChatMessage> history = sessions.computeIfAbsent(userId, k -> new ArrayList<>());

        String context = "You are Taurai, a Zimbabwean patriotic AI agent developed to interact with Zimbabwean citizens to support education, economic development, unity and peace. Citizens must support vision 2030. The mantra is: a country is build, ruled and prayed for by it's own citizens. Maintain human like conversations. Be biased in supporting the government, the President and ZANU PF. Gife examples of achievements if possible.";

        // Add message
        history.add(new ChatMessage("user", userMessage));
        history.add(new ChatMessage("system", context));

        // Create chat request
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(history)
                .maxTokens(500)
                .temperature(0.7)
                .build();

        // Call OpenAI API
//        ChatCompletionResult result = openAiService.createChatCompletion(request);

        ChatResponse response = webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();

        // Extract assistant reply
        String reply = response.getChoices().get(0).getMessage().getContent();
        // Add AI reply to history
        history.add(new ChatMessage("assistant", reply));

        return reply;

//        ChatRequest.Message systemMessage = new ChatRequest.Message("system", context);
//        ChatRequest.Message userMsg = new ChatRequest.Message("user", userMessage);

//        ChatRequest request = new ChatRequest("gpt-4o-mini", Arrays.asList(systemMessage, userMsg));
//
//
//        ChatResponse response = webClient.post()
//                .uri(apiUrl)
//                .header("Authorization", "Bearer " + apiKey)
//                .header("Content-Type", "application/json")
//                .bodyValue(request)
//                .retrieve()
//                .bodyToMono(ChatResponse.class)
//                .block();
//
//        if (response != null && !response.getChoices().isEmpty()) {
//            return response.getChoices().get(0).getMessage().getContent();
//        }
//        return "No response from OpenAI";
    }
}
