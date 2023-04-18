//Zayd Kudaimi  Shinhyung Lee  Steve Rubin 

import java.util.ArrayList;
public class LSP {
    public long time;
    public int senderPort;
    public int seq;
    public int ttl;
    public int receiverFromPort;
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
   
    public LSP(LSP clone) {
        this.time = clone.time;
        this.senderPort = clone.senderPort;
        this.seq = clone.seq;
        this.ttl = clone.ttl;
        this.adjRouterPort = new ArrayList<>(clone.adjRouterPort);
        this.distance = new ArrayList<>(clone.distance);
    }
    public LSP(long time, int senderPort, int seq, int ttl, ArrayList<Integer> adjRouterPort, ArrayList<Integer> distance, int receiverFromPort) {
        this.time = time;
        this.senderPort = senderPort;
        this.seq = seq;
        this.ttl = ttl;
        this.adjRouterPort = adjRouterPort;
        this.distance = distance;
        this.receiverFromPort = receiverFromPort;
    }
    
}
