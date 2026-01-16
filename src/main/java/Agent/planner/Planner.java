package Agent.planner;

import Agent.agent.AgentState;

public interface Planner {
    PlanStep nextStep(AgentState state) throws Exception;
}