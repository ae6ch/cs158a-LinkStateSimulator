//Zayd Kudaimi 015637245 Shinhyung Lee 014175837 Steve Rubin 017439448

public class routeTableEntry {
    private int port;
    private int distance;
    private int nexthop;

    public routeTableEntry(int port, int distance, int nexthop) {
        this.port = port;
        this.distance = distance;
        this.nexthop = nexthop;
    }

    public int getPort() {
        return port;
    }

    public int getDistance() {
        return distance;
    }

    public int getNexthop() {
        return nexthop;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setNexthop(int nexthop) {
        this.nexthop = nexthop;
    }
}