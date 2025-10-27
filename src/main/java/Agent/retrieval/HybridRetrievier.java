package Agent.retrieval;

import Agent.Settings;
import Agent.llm.OpenAiEmbedder;
import Agent.model.DocChunk;

import java.util.*;

public class HybridRetrievier {
    private final Settings settings;
    private final VectorStore vectorStore;
    private final LuceneKeywordIndex keywordIndex;
    private final OpenAiEmbedder embedder;

    public HybridRetrievier(Settings settings, VectorStore vectorStore, LuceneKeywordIndex keywordIndex) {
        this.settings = settings;
        this.vectorStore = vectorStore;
        this.keywordIndex = keywordIndex;
        this.embedder = new OpenAiEmbedder(settings);
    }

    public List<ScoredChunk> retrieve(String query) throws Exception {
        int k = settings.topK();

        float[] qvec = embedder.embedOne(query);
        List<Scored> vec = vectorStore.search(qvec, k);

        List<DocChunk> kw = keywordIndex.search(query, k);

        Map<String, ScoredChunk> byId = new HashMap<>();
        double kwWeight = settings.keywordWeight();

        for (int i = 0; i < vec.size(); i++) {
            Scored s = vec.get(i);
            if (s.score() < settings.minVecScore()) continue;
            byId.put(s.chunk().chunkId(), new ScoredChunk(s.chunk(), s.score(), 0.0));
        }

        for (int i=0;i<kw.size();i++) {
            DocChunk c = kw.get(i);
            double inc = kwWeight * (1.0 - (double)i / Math.max(1, k));
            byId.merge(c.chunkId(), new ScoredChunk(c, 0, inc),
                    (a,b) -> new ScoredChunk(a.chunk(), a.vecScore() + b.vecScore(), a.kwScore() + b.kwScore()));
        }
        List<ScoredChunk> merged = new ArrayList<>();
        merged.sort((a, b) -> Double.compare(b.total(), a.total()));

        LinkedHashMap<String, ScoredChunk> detup = new LinkedHashMap<>();
        for (ScoredChunk s : merged) {
            detup.put(s.chunk().chunkId(), s);
        }
        return detup.values().stream()
                .limit(k)
                .toList();
    }
}
