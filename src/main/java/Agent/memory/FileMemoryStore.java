package Agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileMemoryStore implements MemoryStore {
    private final Path file;
    private final ObjectMapper om = new ObjectMapper();

    public FileMemoryStore(Path file) {
        this.file = file;
    }

    @Override
    public void append(MemoryItem item) throws Exception {
        Files.createDirectories(file.getParent());
        String line = om.writeValueAsString(item) + "\n";
        Files.writeString(file, line,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }
}