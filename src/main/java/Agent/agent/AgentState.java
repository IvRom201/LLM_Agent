package Agent.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgentState {
    private final String goal;
    private final List<AgentMessage> messages = new ArrayList<>();

    public AgentState(String goal) {
        this.goal = goal;
    }

    public String goal() {
        return goal;
    }

    public void add(String role, String content) {
        messages.add(new AgentMessage(role, content));
    }

    public List<AgentMessage> messages() {
        return Collections.unmodifiableList(messages);
    }

    public String lastAssistantMessageOrEmpty() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("assistant".equalsIgnoreCase(messages.get(i).role())) {
                return messages.get(i).content();
            }
        }
        return "";
    }
}