package Agent.qa;

import Agent.Settings;
import Agent.llm.LlmClient;
import Agent.prompt.PromptBuilder;
import Agent.retrieval.HybridRetrievier;

public class QaEngine {
    private final Settings settings;
    private final HybridRetrievier retrivier;
    private final PromptBuilder promptBuilder;
    private final AnswerSynthesizer synthesizer;

    public QaEngine(Settings settings, HybridRetrievier retrivier, PromptBuilder promptBuilder, LlmClient llm) {
        this.settings = settings;
        this.retrivier = retrivier;
        this.promptBuilder = promptBuilder;
        this.synthesizer = new AnswerSynthesizer(settings, promptBuilder, llm);
    }

    public String answer(String question) {
        try{
            var chunks = retrivier.retrieve(question);
            return synthesizer.synthesize(question, chunks);
        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }
}
