package Agent.qa;

import Agent.Settings;
import Agent.llm.LlmClient;
import Agent.prompt.PromptBuilder;
import Agent.retrieval.ScoredChunk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class AnswerSynthesizer {
    private final Settings settings;
    private final PromptBuilder promptBuilder;
    private final LlmClient llm;

    public AnswerSynthesizer(Settings settings, PromptBuilder promptBuilder, LlmClient llm) {
        this.settings = settings;
        this.promptBuilder = promptBuilder;
        this.llm = llm;
    }

    public String synthesize(String question, List<ScoredChunk> chunks) throws Exception {
        int needSources = settings.requireUniqueSources();
        long unique = chunks.stream()
                .map(c -> c.chunk().sourceId())
                .distinct()
                .count();

        if (unique < needSources) {
            return "I cannot answer with certainty: insufficient sources found ("
                    + unique + " < " + needSources + "). "
                    + "Add more relevant documentation or clarify the question.";
        }

        String system = promptBuilder.systemPrompt();
        String user = promptBuilder.userPrompt(question, chunks);

        String raw = llm.complete(system, user, settings.temperature(), settings.maxTokens());

        String src = chunks.stream()
                .collect(Collectors.toMap(
                        c -> c.chunk().sourceId(),
                        c -> c.chunk().sourceTitle(),
                        (a, b) -> a, LinkedHashMap::new
                ))
                .values().stream()
                .map(t -> "- " + t)
                .collect(Collectors.joining("\n"));

        return raw.trim() + "\n\nSources:\n" + src;
    }
}
