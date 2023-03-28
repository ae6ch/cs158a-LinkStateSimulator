import java.util.ArrayList;
public record LSP(int senderPort, int seq, int ttl, ArrayList<Integer> adjRouterPort, ArrayList<Integer> distance) {}
