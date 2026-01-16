package Agent.io;

import Agent.Settings;
import Agent.llm.OpenAiEmbedder;
import Agent.model.DocChunk;
import Agent.retrieval.LuceneKeywordIndex;
import Agent.retrieval.VectorStore;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

public class DocumentIngestor {
    private final Settings settings;
    private final OpenAiEmbedder embedder;
    private final VectorStore vectorStore;
    private final LuceneKeywordIndex keywordIndex;
    private final Chunker chunker;
    private final static Tika tika = new Tika();

    private final Path hashFile = Path.of(".ingest-hashes");

    public DocumentIngestor(Settings settings, OpenAiEmbedder embedder, VectorStore vectorStore, LuceneKeywordIndex keywordIndex) {
        this.settings = settings;
        this.embedder = embedder;
        this.vectorStore = vectorStore;
        this.keywordIndex = keywordIndex;
        this.chunker = new Chunker(settings.chunkChars(), settings.chunkOverlap());
    }

    public void ensureIngested() throws Exception {
        Map<String, String> known = loadHashes();
        Map<String, String> current = new LinkedHashMap<>();

        List<Path> files = listDocFiles(settings.docsDir());

        boolean needsFullRebuild = (vectorStore.size() == 0);

        for (Path p : files) {
            String h = sha1(Files.readAllBytes(p));
            current.put(p.toString(), h);
        }

        boolean anyChanged = needsFullRebuild || !current.equals(known);
        if (!anyChanged) return;

        List<DocChunk> allChunks = new ArrayList<>();
        for (Path p : files) {
            String text = extractText(p);
            String title = stripExt(p.getFileName().toString());
            String sourceId = sha1((p.toAbsolutePath().toString()).getBytes()).substring(0, 10);
            List<String> parts = chunker.split(text);

            for (int i = 0; i < parts.size(); i++) {
                String chunkId = sourceId + "#" + (i + 1);
                allChunks.add(new DocChunk(sourceId, title, chunkId, parts.get(i), null));
            }
        }

        List<float[]> vecs = embedder.embed(allChunks.stream().map(DocChunk::text).toList());
        for (int i = 0; i < allChunks.size(); i++) {
            DocChunk c = allChunks.get(i);
            allChunks.set(i, new DocChunk(c.sourceId(), c.sourceTitle(), c.chunkId(), c.text(), vecs.get(i)));
        }

        vectorStore.clear();
        vectorStore.addAll(allChunks);
        keywordIndex.addAll(allChunks); // internally recreates/wipes
        saveHashes(current);
    }

    public void reingestAll() throws Exception {
        Files.deleteIfExists(hashFile);
        vectorStore.clear();
        keywordIndex.recreate();
        ensureIngested();
    }

    private String extractText(Path p) throws IOException {
        String name = p.getFileName().toString().toLowerCase();
        if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".pdf")) {
            try (InputStream is = Files.newInputStream(p)) {
                return tika.parseToString(is);
            } catch (IOException | TikaException e) {
                System.err.println("Tika was unable to parse " + p + ": " + e.getMessage());
                return "";
            }
        }
        return "";
    }

    private static List<Path> listDocFiles(String dir) throws IOException {
        try (var s = Files.walk(Path.of(dir))) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".txt") || n.endsWith(".md") || n.endsWith(".pdf");
                    })
                    .collect(Collectors.toList());
        }
    }

    private static String stripExt(String s) {
        int i = s.lastIndexOf('.');
        return (i > 0) ? s.substring(0, i) : s;
    }

    private static String sha1(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] d = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private Map<String, String> loadHashes() {
        try {
            if (!Files.exists(hashFile)) return new HashMap<>();
            List<String> lines = Files.readAllLines(hashFile);
            Map<String, String> map = new HashMap<>();
            for (String l : lines) {
                int i = l.indexOf('=');
                map.put(l.substring(0, i), l.substring(i + 1));
            }
            return map;
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    private void saveHashes(Map<String, String> m) {
        try {
            List<String> lines = m.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList();
            Files.write(hashFile, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }
}