package Agent.tools;

import Agent.retrieval.HybridRetrievier;
import Agent.retrieval.ScoredChunk;

import java.util.List;
import java.util.Map;

public class SearchDocsTool implements Tool {
    private final HybridRetrievier retriever;

    public SearchDocsTool(HybridRetrievier retriever) {
        this.retriever = retriever;
    }

    @Override
    public String name() {
        return "search_docs";
    }

    @Override
    public ToolResult execute(Map<String, Object> args) throws Exception {
        String q = (args == null) ? null : (String) args.get("query");
        if (q == null || q.isBlank()) {
            return new ToolResult("ERROR: missing args.query");
        }

        List<ScoredChunk> hits = retriever.retrieve(q);
        if (hits.isEmpty()) {
            return new ToolResult("No relevant chunks were found.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Top matching chunks:\n");
        for (int i = 0; i < hits.size(); i++) {
            ScoredChunk s = hits.get(i);
            String text = s.chunk().text();
            if (text.length() > 900) text = text.substring(0, 900) + "...";
            sb.append("[").append(i + 1).append("] ")
                    .append(s.chunk().sourceTitle()).append(" | ")
                    .append(s.chunk().chunkId())
                    .append(" | score=").append(String.format("%.3f", s.total()))
                    .append("\n")
                    .append(text)
                    .append("\n\n");
        }
        return new ToolResult(sb.toString());
    }
}