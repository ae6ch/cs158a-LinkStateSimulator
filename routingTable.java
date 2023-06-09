//Zayd Kudaimi  Shinhyung Lee  Steve Rubin 

import java.util.LinkedList;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;


public class routingTable {
    private static final String TABLE_FORMAT_STRING = "%10s %10s %10s\n";
    LinkedList<LSP> listLSP;
    int portNumber;

    private HashMap<Integer,routeTableEntry> routeTable;

   
    public routingTable(LinkedList<LSP> listLSP, int portNumber)  {
        routeTable = new HashMap<Integer, routeTableEntry>();
        this.listLSP = listLSP;
        this.portNumber = portNumber;
       
    }
    

    public void setRouteTable(HashMap<Integer,routeTableEntry> routeTable) {
        this.routeTable = routeTable;
    }

    public void addEntry(int destination,routeTableEntry entry) {
        routeTable.put(destination,entry);
    }

    public void removeEntry(int destination) {
        routeTable.remove(destination);
    }

    public String printTable() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(buf);
        PrintStream printf = ps.printf(TABLE_FORMAT_STRING,"Destination","Distance","Nexthop");
        ps.printf(TABLE_FORMAT_STRING,"-----------","--------","-------");
        routeTable.forEach((router,entry) -> {
            String distance = Integer.toString(entry.getDistance());
            String nexthop = Integer.toString(entry.getNexthop());
            if (entry.getNexthop() == -1)
                nexthop = "" + portNumber;
            if (entry.getDistance() == Integer.MAX_VALUE) {
                distance = "\u221e";
                nexthop = "NULL";
            }
                ps.printf(TABLE_FORMAT_STRING,router,distance,nexthop);
        });
        printf.close();
        return buf.toString();
    }
    
    // create a method called calculateTable runs a dijkstra algorithm to calculate the shortest path
    public void calculateTable() {
        //int nh=-1;
        boolean[] perm = new boolean[listLSP.size()];
        List<LSP> listLSPCopy = new ArrayList<LSP>(listLSP);
        for (LSP lsp : listLSPCopy) {
            if(lsp.senderPort == portNumber)
                routeTable.put(lsp.senderPort,new routeTableEntry(lsp.senderPort, 0, -1));
            else 
                routeTable.put(lsp.senderPort,new routeTableEntry(lsp.senderPort, Integer.MAX_VALUE, -1));

        }

        for(int i = 0; i < listLSP.size(); i++){
            
            LSP active = new LSP(0,0,0,0, new ArrayList<Integer>(), new ArrayList<Integer>());
            int min = Integer.MAX_VALUE;
            int index = -1;
            try {
            for (int j = 0; j < listLSP.size(); j++) {
                LSP lsp = listLSP.get(j);
                if(!perm[j]) {
                    routeTableEntry rte = routeTable.get(lsp.senderPort); 
                    //nh = lsp.senderPort;
                    int n = rte.getDistance();
                    if (n < min) {
                        min = n;
                        active = lsp;
                        index = j;
                    }
                 }
            }
            } catch (Exception NullPointerException) {
                System.out.println("Error1: NullPointerException");
            }
            
            if(index > -1){
               perm[index] = true;
               int myDist = routeTable.get(active.senderPort).getDistance();
            
              for (int j = 0; j < active.adjRouterPort.size(); j++){
                    int activeDist;
                    try {
                        activeDist = active.distance.get(j);
                    } catch (Exception NullPointerException) {  // Attempt to recover by max out the distance if there is no distance. 
                        System.out.println("Error2: NullPointerException");
                        activeDist = Integer.MAX_VALUE;
                    } 
                   
                    int portDist = myDist + activeDist;
                    int current;
                    try {
                        current = routeTable.get(active.adjRouterPort.get(j)).getDistance();
                    } catch (Exception NullPointerException) {  // Attempt to recover by max out the distance if there is no distance. 
                        //System.out.println("Error3: NullPointerException");
                        current = Integer.MAX_VALUE;
                    }
                    if(portDist < current){
                        try {
                            if ( active.senderPort == portNumber)
                            routeTable.put(active.adjRouterPort.get(j),new routeTableEntry(active.adjRouterPort.get(j), portDist,active.adjRouterPort.get(j)));
                        else
                            routeTable.put(active.adjRouterPort.get(j),new routeTableEntry(active.adjRouterPort.get(j), portDist, routeTable.get(active.senderPort).getNexthop()));
                        } catch (Exception NullPointerException) {  // Attempt to recover  )
                            //System.out.println("Error4: NullPointerException");
                            //routeTable.put(active.adjRouterPort.get(j),entry);
                        }
                        

                    }
                }
            }
            
        }
    }
}
