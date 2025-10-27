package Agent.retrieval;

import Agent.model.DocChunk;

public record ScoredChunk(
        DocChunk chunk,
        double vecScore,
        double kwScore
) {
    public double total(){
        return vecScore + kwScore;
    }
}
