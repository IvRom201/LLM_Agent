package Agent.tools;

import java.util.Map;

public interface Tool {
    String name();

    ToolResult execute(Map<String, Object> args) throws Exception;
}