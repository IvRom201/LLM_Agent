package Agent;

import Agent.io.DocumentIngestor;
import Agent.llm.LlmClient;
import Agent.llm.OpenAiChatClient;
import Agent.llm.OpenAiEmbedder;
import Agent.agent.AgentLoop;
import Agent.memory.FileMemoryStore;
import Agent.memory.MemoryStore;
import Agent.planner.LlmPlanner;
import Agent.planner.Planner;
import Agent.prompt.PromptBuilder;
import Agent.qa.QaEngine;
import Agent.retrieval.HybridRetrievier;
import Agent.retrieval.LuceneKeywordIndex;
import Agent.retrieval.VectorStore;
import Agent.tools.SearchDocsTool;
import Agent.tools.SaveNoteTool;
import Agent.tools.ToolExecutor;
import Agent.tools.ToolRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
        Settings settings = Settings.load();

        Path docsDir = Path.of(settings.docsDir());
        if (!Files.exists(docsDir)) {
            System.out.println("The docs folder was not found: " + docsDir.toAbsolutePath());
            System.out.println("Create it and place it in .md/.txt/.pdf");
            return;
        }

        VectorStore vectorStore = new VectorStore();
        LuceneKeywordIndex keywordIndex = new LuceneKeywordIndex(Path.of(settings.indexDir()));

        OpenAiEmbedder embedder = new OpenAiEmbedder(settings);
        DocumentIngestor ingestor = new DocumentIngestor(settings, embedder, vectorStore, keywordIndex);
        ingestor.ensureIngested();

        LlmClient llm = switch (settings.providerName()) {
            case "openai" -> new OpenAiChatClient(settings);
            default -> {
                System.out.println("So far, only openai has been implemented");
                yield new OpenAiChatClient(settings);
            }
        };

        HybridRetrievier retriever = new HybridRetrievier(settings, vectorStore, keywordIndex);
        PromptBuilder promptBuilder = new PromptBuilder(settings);
        QaEngine engine = new QaEngine(settings, retriever, promptBuilder, llm);

        // --- Agent layer (planner + tools) ---
        MemoryStore memoryStore = new FileMemoryStore(Path.of("memory", "notes.jsonl"));
        ToolRegistry registry = new ToolRegistry()
                .register(new SearchDocsTool(retriever))
                .register(new SaveNoteTool(memoryStore));
        ToolExecutor toolExecutor = new ToolExecutor(registry);
        Planner planner = new LlmPlanner(settings, llm);
        AgentLoop agent = new AgentLoop(planner, toolExecutor, 6);

        System.out.println("Agent is running");
        System.out.println("Commands: :reload (reindex), :qa (one-shot RAG), :quit");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n> ");
            String line = scanner.nextLine().trim();
            if (line.equals(":reload")) {
                ingestor.reingestAll();
                System.out.println("Done");
                continue;
            }
            if (line.equals(":quit")) {
                break;
            }

            if (line.equals(":qa")) {
                System.out.println("Enter question:");
                String q = scanner.nextLine().trim();
                String ans = engine.answer(q);
                System.out.println(ans);
                continue;
            }

            // Default: run multi-step agent loop
            String ans;
            try {
                ans = agent.run(line);
            } catch (Exception e) {
                // fallback to direct QA to keep the UX resilient
                ans = engine.answer(line);
            }
            System.out.println(ans);
        }
    }
}