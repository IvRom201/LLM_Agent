package Agent.model;

public record DocChunk(
        String sourceId,
        String sourceTitle,
        String chunkId,
        String text,
        float[] embedding
) {}
