package Agent.memory;

import Agent.memory.MemoryItem;
import Agent.memory.MemoryStore;

import java.time.Instant;
import java.util.Map;

public class SaveNoteTool implements Tool {
    private final MemoryStore store;

    public SaveNoteTool(MemoryStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "save_note";
    }

    @Override
    public ToolResult execute(Map<String, Object> args) throws Exception {
        String text = (args == null) ? null : (String) args.get("text");
        String tags = (args == null) ? null : (String) args.get("tags");

        if (text == null || text.isBlank()) {
            return new ToolResult("ERROR: missing args.text");
        }

        store.append(new MemoryItem(text.trim(), tags, Instant.now()));
        return new ToolResult("Saved.");
    }
}