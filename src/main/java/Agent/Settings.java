package Agent;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public record Settings (
    String providerName,
    String chatModel,
    String embedModel,
    String baseUrl,
    String docsDir,
    String indexDir,
    int chunkChars,
    int chunkOverlap,
    int topK,
    double minVecScore,
    double keywordWeight,
    int requireUniqueSources,
    double temperature,
    int maxTokens,
    String systemRules,
    String answerHeader
) {
    @SuppressWarnings("unchecked")
    public static Settings load() throws Exception {
        Yaml yaml = new Yaml();
        try(InputStream in = Settings.class.getResourceAsStream("/application.yaml")){

            if (in == null) {
                throw new IllegalStateException(
                        "application.yaml not found on classpath. " +
                                "Expected at src/main/resources/application.yaml"
                );
            }

            Map<String, Object> root = yaml.load(in);
            if (root == null || root.isEmpty()) {
                throw new IllegalStateException("application.yaml is empty or invalid YAML");
            }

            Map<String, Object> provider = (Map<String, Object>) root.get("provider");
            Map<String, Object> paths = (Map<String, Object>) root.get("paths");
            Map<String, Object> retrieval = (Map<String, Object>) root.get("retrieval");
            Map<String, Object> generation = (Map<String, Object>) root.get("generation");
            Map<String, Object> ui = (Map<String, Object>) root.get("ui");

            if (provider == null) throw new IllegalStateException("Missing 'provider' section in application.yaml");
            if (paths == null) throw new IllegalStateException("Missing 'paths' section in application.yaml");
            if (retrieval == null) throw new IllegalStateException("Missing 'retrieval' section in application.yaml");
            if (generation == null) throw new IllegalStateException("Missing 'generation' section in application.yaml");
            if (ui == null) throw new IllegalStateException("Missing 'ui' section in application.yaml");


            return new Settings(
                    (String) provider.get("name"),
                    (String) provider.get("chat_model"),
                    (String) provider.get("embed_model"),
                    (String) provider.get("base_url"),
                    (String) paths.get("docs_dir"),
                    (String) paths.get("index_dir"),
                    (Integer) retrieval.get("chunk_chars"),
                    (Integer) retrieval.get("chunk_overlap"),
                    (Integer) retrieval.get("top_k"),
                    ((Number) retrieval.get("min_vec_score")).doubleValue(),
                    ((Number) retrieval.get("keyword_weight")).doubleValue(),
                    (Integer) retrieval.get("require_unique_sources"),
                    ((Number) generation.get("temperature")).doubleValue(),
                    (Integer) generation.get("max_tokens"),
                    (String) generation.get("system_rules"),
                    (String) ui.get("answer_header")
            );
        }
    }
}
