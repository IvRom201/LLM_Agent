package Agent.planner;

import Agent.Settings;
import Agent.agent.AgentMessage;
import Agent.agent.AgentState;
import Agent.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class LlmPlanner implements Planner {
    private final Settings settings;
    private final LlmClient llm;
    private final ObjectMapper om = new ObjectMapper();

    public LlmPlanner(Settings settings, LlmClient llm) {
        this.settings = settings;
        this.llm = llm;
    }

    @Override
    public PlanStep nextStep(AgentState state) throws Exception {
        String system = "You are an autonomous planning module for a console AI agent. " +
                "You must respond ONLY with valid JSON and nothing else. " +
                "You can choose one of the actions: \"search_docs\", \"save_note\", \"final\". " +
                "- search_docs: use when you need factual context from the local document corpus. args: {query: string}. " +
                "- save_note: use to store a short useful memory for later. args: {text: string, tags: string(optional)}. " +
                "- final: use when you can answer the user. Provide finalAnswer. done=true. " +
                "JSON schema: {action: string, args: object, finalAnswer: string|null, done: boolean}.";

        StringBuilder ctx = new StringBuilder();
        ctx.append("GOAL:\n").append(state.goal()).append("\n\n");
        ctx.append("CONVERSATION SO FAR:\n");
        for (AgentMessage m : state.messages()) {
            ctx.append(m.role()).append(": ").append(m.content()).append("\n");
        }

        String raw = llm.complete(system, ctx.toString(), 0.2, Math.min(settings.maxTokens(), 800));

        JsonNode n = om.readTree(raw);
        String action = textOrNull(n.get("action"));
        boolean done = n.has("done") && n.get("done").asBoolean(false);
        String finalAnswer = n.hasNonNull("finalAnswer") ? n.get("finalAnswer").asText() : null;

        Map<String, Object> args = new LinkedHashMap<>();
        JsonNode a = n.get("args");
        if (a != null && a.isObject()) {
            a.fields().forEachRemaining(e -> args.put(e.getKey(), nodeToJava(e.getValue())));
        }

        if (action == null || action.isBlank()) {
            return new PlanStep("final", Map.of(), raw, true);
        }
        return new PlanStep(action, args, finalAnswer, done);
    }

    private static String textOrNull(JsonNode n) {
        return (n == null || n.isNull()) ? null : n.asText(null);
    }

    private static Object nodeToJava(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isTextual()) return n.asText();
        if (n.isBoolean()) return n.asBoolean();
        if (n.isInt() || n.isLong()) return n.asLong();
        if (n.isFloat() || n.isDouble() || n.isBigDecimal()) return n.asDouble();
        return n.toString();
    }
}