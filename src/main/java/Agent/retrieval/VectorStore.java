package Agent.retrieval;

import Agent.model.DocChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import static org.apache.lucene.util.VectorUtil.cosine;

public class VectorStore {
    private final List<DocChunk> chunks = new ArrayList<>();

    public void addAll(List<DocChunk> list) {
        chunks.addAll(list);
    }
    public void clear(){
        chunks.clear();
    }

    public List<Scored> search(float[] q, int k){
        PriorityQueue<Scored> pq = new PriorityQueue<>(Comparator.comparingDouble(Scored::score));
        for (DocChunk c : chunks) {
            if(c.embedding() == null) continue;
            double s = cosine(q, c.embedding());
            Scored sc = new Scored(c, s);
            if(pq.size() < k) {
                pq.add(sc);
            } else if(s > pq.peek().score()){
                pq.poll();
                pq.add(sc);
            }
        }
        List<Scored> res = new ArrayList<>(pq);
        res.sort((a,b) -> Double.compare(b.score(), a.score()));
        return res;
    }

    private static double cosine(float[] a, float[] b){
        double dot = 0, na = 0, nb = 0;
        for(int i = 0; i < a.length; i++){
            dot += a[i]*b[i];
            na += a[i]*a[i];
            nb += b[i]*b[i];
        }
        return (na==0 || nb ==0) ? 0 : dot/Math.sqrt(na*nb);
    }

}
