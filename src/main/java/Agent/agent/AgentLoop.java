package Agent.agent;

import Agent.planner.PlanStep;
import Agent.planner.Planner;
import Agent.tools.ToolExecutor;
import Agent.tools.ToolResult;

public class AgentLoop {
    private final Planner planner;
    private final ToolExecutor tools;
    private final int maxSteps;

    public AgentLoop(Planner planner, ToolExecutor tools, int maxSteps) {
        this.planner = planner;
        this.tools = tools;
        this.maxSteps = maxSteps;
    }

    public String run(String userInput) throws Exception {
        AgentState state = new AgentState(userInput);
        state.add("user", userInput);

        for (int step = 1; step <= maxSteps; step++) {
            PlanStep ps = planner.nextStep(state);

            String action = ps.action();
            if (action == null) action = "final";

            if (ps.done() || "final".equalsIgnoreCase(action)) {
                String answer = ps.finalAnswer();
                if (answer == null || answer.isBlank()) {
                    answer = "(No finalAnswer was produced.)";
                }
                state.add("assistant", answer);
                return answer;
            }

            ToolResult tr = tools.execute(action, ps.args());
            state.add("tool", "tool=" + action + "\n" + tr.observation());
        }

        return "Stopped: reached maxSteps=" + maxSteps;
    }
}