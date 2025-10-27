package Agent.prompt;

import Agent.Settings;
import Agent.retrieval.ScoredChunk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromptBuilder {
    private final Settings settings;

    public PromptBuilder(Settings settings) {
        this.settings = settings;
    }

    public String systemPrompt() {
        return settings.systemRules();
    }

    public String userPrompt(String question, List<ScoredChunk> chunks) {
        StringBuilder ctx = new StringBuilder();
        Map<String,Integer> sourceIndex = new LinkedHashMap<>();
        int sCounter = 1;
        for (var sc : chunks) {
            String key = sc.chunk().sourceId();
            if (!sourceIndex.containsKey(key)) sourceIndex.put(key, sCounter++);
        }

        for (var sc : chunks) {
            int s = sourceIndex.get(sc.chunk().sourceId());
            ctx.append("\n[S").append(s).append("] ")
                    .append("«").append(trim(sc.chunk().text(), 1200)).append("»")
                    .append(" — source: ").append(sc.chunk().sourceTitle())
                    .append(", fragment ").append(sc.chunk().chunkId())
                    .append("\n");
        }

        String srcList = sourceIndex.entrySet().stream()
                .map(e -> "[S"+e.getValue()+"] " + chunks.stream()
                        .filter(c -> c.chunk().sourceId().equals(e.getKey()))
                        .findFirst().get().chunk().sourceTitle())
                .distinct()
                .collect(Collectors.joining("\n"));

        return settings.answerHeader() + "\n"
                + "User question: " + question + "\n\n"
                + "Context from local documentation (minimum of 3 different sources):\n"
                + ctx + "\n"
                + "List of sources:\n" + srcList + "\n\n"
                + "Instructions for responding:\n"
                + "- Use only the fragments provided.\n"
                + "- Provide citations and notes [S#] next to the facts..\n"
                + "- If something is missing, please be honest about what exactly is missing and ask for clarification/additional documents..\n";
    }

    public static String trim(String str, int maxLength) {
        if(str == null) return "";
        if(str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + " ...";
    }
}
