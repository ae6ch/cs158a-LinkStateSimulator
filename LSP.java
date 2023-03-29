import java.util.ArrayList;
//public record LSP(long time, int senderPort, int seq, int ttl, ArrayList<Integer> adjRouterPort, ArrayList<Integer> distance) { }
public class LSP {
    public long time;
    public int senderPort;
    public int seq;
    public int ttl;
    public ArrayList<Integer> adjRouterPort;
    public ArrayList<Integer> distance;
    public LSP(long time, int senderPort, int seq, int ttl, ArrayList<Integer> adjRouterPort, ArrayList<Integer> distance) {
        this.time = time;
        this.senderPort = senderPort;
        this.seq = seq;
        this.ttl = ttl;
        this.adjRouterPort = adjRouterPort;
        this.distance = distance;
    }
    public String toString() {
        return "LSP: time=" + time + " senderPort=" + senderPort + " seq=" + seq + " ttl=" + ttl + " adjRouterPort=" + adjRouterPort + " distance=" + distance;
    }
}