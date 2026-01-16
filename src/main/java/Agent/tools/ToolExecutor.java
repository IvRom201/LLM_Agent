package Agent.tools;

import java.util.Map;

public class ToolExecutor {
    private final ToolRegistry registry;

    public ToolExecutor(ToolRegistry registry) {
        this.registry = registry;
    }

    public ToolResult execute(String toolName, Map<String, Object> args) throws Exception {
        Tool tool = registry.get(toolName);
        if (tool == null) {
            return new ToolResult("ERROR: unknown tool: " + toolName);
        }
        try {
            return tool.execute(args);
        } catch (Exception e) {
            return new ToolResult("ERROR executing tool '" + toolName + "': " + e.getMessage());
        }
    }
}