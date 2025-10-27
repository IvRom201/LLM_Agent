package Agent.llm;

import Agent.Settings;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiChatClient implements LlmClient{
    private final Settings settings;
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    public OpenAiChatClient(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt, double temperature, int maxTokens) throws Exception{
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OPENAI_API_KEY not set");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", settings.chatModel());
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        Request request = new Request.Builder()
                .url(settings.baseUrl() + "/chat/complections")
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(om.writeValueAsBytes(body), MediaType.parse("application/json")))
                .build();

        try (Response resp = http.newCall(request).execute()) {
            if (!resp.isSuccessful()) throw new RuntimeException("OpenAI error: " + resp.code() + " " + resp.message());
            var tree = om.readTree(resp.body().byteStream());
            return tree.get("choices").get(0).get("message").get("content").asText();
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        throw new UnsupportedOperationException();
    }
}
