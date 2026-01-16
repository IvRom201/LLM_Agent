package Agent.memory;

import java.time.Instant;

public record MemoryItem(String text, String tags, Instant timestamp) {
}