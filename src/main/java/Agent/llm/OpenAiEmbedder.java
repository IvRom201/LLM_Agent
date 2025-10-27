package Agent.llm;

import Agent.Settings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenAiEmbedder {
    private final Settings settings;
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiEmbedder(Settings settings) {
        this.settings = settings;
    }

    public List<float[]> embed(List<String> texts) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) throw new Exception("OPENAI_API_KEY not set");

        var body = Map.of(
                "model",
                settings.embedModel(),
                "input",
                texts
        );

        Request request = new Request.Builder()
                .url(settings.baseUrl() + "/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(objectMapper.writeValueAsBytes(body), MediaType.parse("application/json")))
                .build();

        try (Response response = http.newCall(request).execute()){
            ResponseBody rb = response.body();
            if (!response.isSuccessful()) {
                String err = (rb != null) ? new String(rb.bytes(), StandardCharsets.UTF_8) : "";
                throw new RuntimeException("Embedding error: " + response.code() + " " + response.message() + " " + err);
            }
            if (rb == null) {
                throw new RuntimeException("Embedding error: empty response body");
            }

            JsonNode root = objectMapper.readTree(rb.byteStream());
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                throw new RuntimeException("Embedding error: 'data' is missing or not an array");
            }

            List<float[]> result = new ArrayList<>();
            for (JsonNode node : data) {
                JsonNode arr = node.get("embedding");
                if (arr == null || !arr.isArray()) continue;

                float[] v = new float[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    v[i] = (float) arr.get(i).asDouble();
                }
                result.add(v);
            }
            return result;
        }
    }

    public float[] embedOne(String text) throws Exception {
        return embed(List.of(text)).getFirst();
    }

}
