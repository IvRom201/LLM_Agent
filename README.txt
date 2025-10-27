Architecture:
    User Query
       ↓
    HybridRetriever
       ├─ VectorStore (semantic search via embeddings)
       └─ LuceneKeywordIndex (lexical/BM25 search)
       ↓ (merge, score, dedupe)
    Top-K ScoredChunks
       ↓ (guard: requireUniqueSources)
    AnswerSynthesizer
       ├─ PromptBuilder (system + user prompt)
       └─ LlmClient (chat completion)
       ↓
    Answer + Sources

Key modules:
    DocumentIngestor: loads files from paths.docs_dir, extracts text (Apache Tika), chunks content, computes embeddings, updates Lucene/Vector indexes (hash-based incremental ingestion).
    VectorStore: cosine similarity search over chunk embeddings.
    LuceneKeywordIndex: BM25 search over chunk text.
    HybridRetriever: merges vector + keyword results with tunable weights and minimum semantic score.
    AnswerSynthesizer: enforces n distinct sources (requireUniqueSources), builds prompts, appends a “Sources” list.
    Settings: central config (application.yaml) for models, thresholds, paths, and generation params.

Why these choices:
    RAG on pure Java: demonstrates a full JVM pipeline (no Python), easy to embed in enterprise stacks.
    Apache Tika: robust, multi-format text extraction (.md/.txt/.pdf) without custom parsers.
    Hybrid retrieval (semantic + lexical): balances meaning (embeddings) and exact matching (BM25).
    Hash-based ingestion: reindexes only changed files; fast feedback loops.
    Source guard: requireUniqueSources prevents single-source hallucinations.

Data & Formats:
    Place docs in the folder from application.yaml → paths.docs_dir (e.g., ./docs). Supported: .md, .txt, .pdf.
    Typical structure:
        docs/
         ├─ troubleshooting.txt
         ├─ integration_tips.txt
         ├─ config_guide.txt
         └─ quick_start.txt

Configuration (application.yaml):
    Key settings:
        paths.docs_dir, paths.index_dir
        retrieval.chunk_chars, retrieval.chunk_overlap, retrieval.top_k
        retrieval.min_vec_score, retrieval.keyword_weight, retrieval.require_unique_sources
        generation.temperature, generation.max_tokens, generation.system_rules
        Provider: provider.name=openai, chat_model, embed_model, base_url

Failure modes (and behavior):
    Too few sources ⇒ “I cannot answer with certainty…” (no guessing).
    Scanned PDFs (no text layer) ⇒ add OCR’d PDFs or an .md summary.
    401/429 embedding errors ⇒ fix API key or rate-limiting.

Future possible improvements:
    Embedding cache (persist vectors on disk; avoid re-embedding).
    Source highlighting (inline markers like [S1], [S2]).
    Web UI / REST (Spring Boot/Jetty endpoint for chat).
    Eval harness (gold Q/A set; compute hit rate/precision).
    Streaming (partial answers, token-by-token).