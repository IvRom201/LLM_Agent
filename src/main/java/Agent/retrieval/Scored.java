package Agent.retrieval;

import Agent.model.DocChunk;

public record Scored(
        DocChunk chunk,
        double score
) {}
