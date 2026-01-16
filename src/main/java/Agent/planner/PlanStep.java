package Agent.planner;

import java.util.Map;

public record PlanStep(
        String action,
        Map<String, Object> args,
        String finalAnswer,
        boolean done
) {
}