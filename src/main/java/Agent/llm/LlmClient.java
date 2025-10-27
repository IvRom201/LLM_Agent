package Agent.llm;

import java.util.List;

public interface LlmClient {
    String complete(String systemPrompt, String userPrompt, double temperature, int maxTokens) throws Exception;

    List<float[]> embed(List<String> texts) throws Exception;
}
