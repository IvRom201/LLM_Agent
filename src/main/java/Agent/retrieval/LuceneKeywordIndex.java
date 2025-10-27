package Agent.retrieval;

import Agent.model.DocChunk;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;            // <-- правильный Document из Lucene
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;       // <-- классический QueryParser
import org.apache.lucene.queryparser.classic.QueryParserBase;  // <-- для escape(...)
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;                          // <-- правильный Query из Lucene
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LuceneKeywordIndex {
    private final Path dir;
    private FSDirectory directory;
    private Analyzer analyzer;

    public LuceneKeywordIndex(Path dir) {
        this.dir = dir;
    }

    public void addAll(List<DocChunk> chunks) throws IOException {
        // ensure directory exists/created new
        try (IndexWriter ignored = openWriter(true)) { /* wipe/create */ }
        try (IndexWriter w = openWriter(false)) {
            for (DocChunk c : chunks) {
                Document d = new Document();
                d.add(new StringField("chunkId", c.chunkId(), Field.Store.YES));
                d.add(new StringField("sourceId", c.sourceId(), Field.Store.YES));
                d.add(new StoredField("sourceTitle", c.sourceTitle()));
                d.add(new TextField("text", c.text(), Field.Store.YES));
                w.addDocument(d);
            }
            w.commit();
        }
    }

    public List<DocChunk> search(String query, int k) throws Exception {
        ensureOpen();
        QueryParser parser = new QueryParser("text", analyzer);
        // Экранируем потенциально опасные символы
        Query q = parser.parse(QueryParserBase.escape(query));

        try (DirectoryReader r = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(r);
            TopDocs td = searcher.search(q, k);
            List<DocChunk> res = new ArrayList<>();
            for (ScoreDoc sd : td.scoreDocs) {
                Document d = searcher.doc(sd.doc);
                res.add(new DocChunk(
                        d.get("sourceId"),
                        d.get("sourceTitle"),
                        d.get("chunkId"),
                        d.get("text"),
                        null
                ));
            }
            return res;
        }
    }

    public void recreate() throws IOException {
        if (directory != null) directory.close();
        directory = FSDirectory.open(dir);
        analyzer = new StandardAnalyzer();
        try (IndexWriter w = openWriter(true)) { /* wipe */ }
    }

    private IndexWriter openWriter(boolean create) throws IOException {
        if (directory == null) {
            directory = FSDirectory.open(dir);
            analyzer = new StandardAnalyzer();
        }
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        if (create) cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        return new IndexWriter(directory, cfg);
    }

    private void ensureOpen() throws IOException {
        if (directory == null) {
            directory = FSDirectory.open(dir);
            analyzer = new StandardAnalyzer();
        }
    }
}
