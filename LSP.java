import java.util.ArrayList;
public record LSP(long time, int senderPort, int seq, int ttl, ArrayList<Integer> adjRouterPort, ArrayList<Integer> distance) { }
