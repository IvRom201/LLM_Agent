package Agent.io;

import java.util.ArrayList;
import java.util.List;

public class Chunker {
    private final int size;
    private final int overlap;

    public Chunker(int size, int overlap) {
        this.size = size;
        this.overlap = overlap;
    }

    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if(text == null || text.isBlank()) return chunks;
        int n = text.length();
        int i = 0;
        while (i < n) {
            int end = Math.min(i + size, n);
            String chunk = text.substring(i, end);
            chunks.add(chunk);
            if(end == n) break;
            i = Math.max(end - overlap, i + 1);
        }
        return chunks;
    }
}
