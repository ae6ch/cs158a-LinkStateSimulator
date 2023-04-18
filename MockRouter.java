//Zayd Kudaimi  Shinhyung Lee  Steve Rubin 


import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.concurrent.*;


import java.util.*;
import java.time.*;

public class MockRouter  {
    private static final char CMD_HELLO = 'H';
    private static final char CMD_KEEPALIVE = 'k';
    private static final char CMD_LSP = 'l';
    private static final char CMD_HISTORY = 'h';
    private static final char CMD_STOP = 's';
    private static final int INITIAL_SEQ = 0; // The initial TTL for a LSP
    private static final int INITIAL_TTL = 60; // The initial TTL for a LSP
    private static final int LSA_REFRESH_TIME = 30; // The time between sending our own LSP announcements
    private static final int STABLE_TIME = 5000; // How long we have to be stable before we can calculate the routing table
    LSP myLSA;
    public final Runnable Listener;
    public final Runnable Initiator;
    int portNumber;  
    String[] adjacents;
    boolean keepRunning=true;
    long startTime = Instant.now().toEpochMilli();
    int nextSeq = 1; 
    
    boolean routeTableRecalcNeeded = false; // Do we need to recalculate the routing table? (set to true every time we get a new LSP)
    LinkedList<LSP> listLSP = new LinkedList<>();
    LinkedList<LSP> listLSPHistory = new LinkedList<>();
    long timeOfLastLSP=Instant.now().toEpochMilli();  // Last time we received a LSP packet, used to calculate if LSPs are stable
    long stableTime=0; // How long have we been stable? Updated by the TTL expire thread for now, need to move to the watchdog thread for calculating the routing table 
    routingTable routeTable = new routingTable(listLSP,portNumber);


    // Start the background processes
    ScheduledExecutorService ttlAger = Executors.newSingleThreadScheduledExecutor();  // This is the TTL Age task
    ScheduledExecutorService lsaRefresh = Executors.newSingleThreadScheduledExecutor();  // This is the lsaRefresh task
private synchronized void sendRefresh(LSP myLSA){
        myLSA.time=Instant.now().toEpochMilli()-startTime;
        myLSA.seq++;
        myLSA.ttl=INITIAL_TTL;
    
        if(listLSP.contains(myLSA)) { 
            listLSP.set(listLSP.indexOf(myLSA), myLSA);
        }
        else {
            listLSP.add(myLSA);
        }
        listLSPHistory.add(new LSP(myLSA)); // Add a copy of myLSA to the history list too. We don't want to add a reference to myLSA because it will change as we update it.
    }

private synchronized String lsaListPW(List <LSP> lspList) {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(buf);
    for (LSP lsp : lspList) {
        ps.printf("T+%s %d %d %d", Long.toUnsignedString(lsp.time),lsp.senderPort,lsp.seq,lsp.ttl);
        for (int x=0; x < lsp.adjRouterPort.size(); x++)  {
            ps.printf(" %d-%d",lsp.adjRouterPort.get(x),lsp.distance.get(x));
        }
        ps.println();
    }

    return buf.toString();

}

private void receiveLSP(int remotePort, PrintWriter pw, String command) {
    String[] chunks = command.split(" ");
    boolean dupSeq = false;
    ArrayList<Integer> adjRouterPorts = new ArrayList<>(100);
    ArrayList<Integer> distances = new ArrayList<>(100);
    long timeReceived = Instant.now().toEpochMilli()-startTime;
    for (int x=4; x < chunks.length; x++) {
        String[] adjRouterPortDistance = chunks[x].split("-");
        adjRouterPorts.add(Integer.parseInt(adjRouterPortDistance[0]));
        distances.add(Integer.parseInt(adjRouterPortDistance[1]));
    }   
    
    listLSPHistory.add(new LSP(timeReceived,Integer.parseInt(chunks[1]),Integer.parseInt(chunks[2]),Integer.parseInt(chunks[3]),adjRouterPorts,distances));

    List<LSP> listLSPCopy = new LinkedList<LSP>(listLSP); // Make a copy of the list so we can iterate through it without getting a concurrent modification exception
    for (LSP lspdb : listLSPCopy) {
    // Find existing LSP for router
        if (lspdb.senderPort == Integer.parseInt(chunks[1])) {
            if (lspdb.seq == Integer.parseInt(chunks[2])) { // If the sequence number is the same, igore it
                dupSeq = true;
                
            } 
            else if (lspdb.seq <= Integer.parseInt(chunks[2])) { // If the sequence number is same or newer update it
                lspdb.time=timeReceived;
                lspdb.seq=Integer.parseInt(chunks[2]);
                lspdb.ttl=Integer.parseInt(chunks[3]);
                lspdb.adjRouterPort=adjRouterPorts;
                lspdb.distance=distances;
                dupSeq = true;
                timeOfLastLSP = Instant.now().toEpochMilli();
                routeTableRecalcNeeded = true;
            } 
            else if ( (lspdb.seq == Integer.parseInt(chunks[2])) && Integer.parseInt(chunks[3]) <= 0) { // If the sequence number is the same and ttl=0, delete it from table
                dupSeq = true;
                listLSP.remove(listLSP.indexOf(lspdb)); 
                routeTableRecalcNeeded = true;
 
            } 
            else if (lspdb.seq > Integer.parseInt(chunks[2])) {   // If the sequence number is older, ignore it
                dupSeq = true;
            }
        }
    }
    int ttl = Integer.parseInt(chunks[3]);
    if (ttl > 0 && dupSeq == false) {  
        timeOfLastLSP = Instant.now().toEpochMilli();
        routeTableRecalcNeeded = true;
        LSP lsp = new LSP(timeReceived,Integer.parseInt(chunks[1]),Integer.parseInt(chunks[2]),Integer.parseInt(chunks[3]),adjRouterPorts,distances,remotePort);
        listLSP.add(lsp);
    } 
    
    pw.println("ACK");
    pw.flush();
}

private void handleStop(Socket s, PrintWriter pw) throws IOException {
    pw.println("STOPPING");
    pw.flush();
    s.close();
    keepRunning=false;
    ttlAger.shutdown();
    lsaRefresh.shutdown();
    Thread.currentThread().interrupt();
}

private void printTable(PrintWriter pw) {
    pw.printf("HISTORY\n----------------------------------------\n%s\n",lsaListPW(listLSPHistory));
    pw.flush();

    pw.printf("CURRENT\n----------------------------------------\n%s\n",lsaListPW(listLSP));
    pw.flush();

    pw.printf("TABLE\n---------------------------------------\n%s\n",routeTable.printTable());
    pw.flush();
}
public MockRouter(int portNumber, String[] adjacents)  { // adjacents is port-distance port-distace port-distace 
        this.adjacents = adjacents;
        this.portNumber = portNumber;
        
        // Initialize the LSP for this router, we will add to the myAdjRouterPorts and myDistances as sockets come up
        ArrayList<Integer> myAdjRouterPorts = new ArrayList<Integer>(100);
        ArrayList<Integer> myDistances = new ArrayList<Integer>(100);
        myLSA = new LSP(Instant.now().toEpochMilli()-startTime, portNumber, INITIAL_SEQ,INITIAL_TTL, myAdjRouterPorts, myDistances);      
        lsaRefresh.scheduleAtFixedRate(new LsaRefresh(), 0, LSA_REFRESH_TIME, TimeUnit.SECONDS);
        ttlAger.scheduleAtFixedRate(new TtlAger(), 0, 1, TimeUnit.SECONDS);


        Listener = new ServerThread(portNumber);

        Initiator = new ClientThread(portNumber, adjacents);
        
    }

private final class ClientThread implements Runnable {
        private final int portNumber;
        private final String[] adjacents;
        HashMap<Integer,SeqRecord> hashseqRecord = new HashMap<>();


        private ClientThread(int portNumber, String[] adjacents) {
            this.portNumber = portNumber;
            this.adjacents = adjacents;
        }

        public void run() {             // CLIENT CODE GOES HERE

            Thread.currentThread().setName("[" + Integer.toString(portNumber) + "] Initiator");

            while (keepRunning) processOutgoingConnections();
            

        }

        private void processOutgoingConnections() {
            // Recalculate the routing table if we have been stable for a while
                recalculateTableIfStable();

                float sleepyTime=(float) ((Math.random()*1000)+3000);

                try { 
                    Thread.sleep((long)sleepyTime);
                } catch (Exception ignore) {};
                String[] endpoints = adjacents;
                for (String e: endpoints) {     // Main outgoing connection loop here
                    String[] ep = e.split("-");
                    int port = Integer.parseInt(ep[0]);
                    int distance = Integer.parseInt(ep[1]);
                    // Find seqRecord for this ep, if it doesnt exist, insert a new one with a empty seqAck table
                     
                    if (!hashseqRecord.containsKey(port)) {
                        HashMap<Integer,Integer> sa = new HashMap<Integer,Integer>();
                        SeqRecord sr = new SeqRecord(port,sa);
                        hashseqRecord.put(port,sr);
                    }
                    SeqRecord sr = hashseqRecord.get(port);
                    try {
                     
                      sendLSPs(port, distance, sr);
                    } 
                    catch (ConnectException ce) {
                        dropNeighborFromAdj(port);
                    }
                    catch (Exception ignore) {
                        //System.out.printf("[%d] Error4: %s\n",portNumber,ignore);
                    } 
                }           
            
                cleanLSPTable();
        }

        private void sendLSPs(int port, int distance, SeqRecord sr)
                throws UnknownHostException, IOException, SocketException {
            // loop through lsp list, if there is a lsp, and the seq number is greater than the seqAck then send it
              List<LSP> listLSPcopy = new ArrayList<LSP>(listLSP); // iterate over a copy because concurrency 
              for (LSP lsp : listLSPcopy) {
                // If we dont have a seqAck for this senderPort, then add it
                if (!sr.seqAck().containsKey(lsp.senderPort))
                    sr.seqAck().put(lsp.senderPort,-1 );
              
                if ((sr.seqAck().get(lsp.senderPort) >= lsp.seq) && (lsp.ttl > 0)) { // We've already send this LSP and its not expired
                                                                                     // if lsp.ttl >0 is how we reflood a 0 TTL LSP
                    continue;
                }
                // Send the LSP
                Socket s = new Socket("localhost", port);
                s.setSoTimeout(10000); // Try to prevent BufferedReader and PrintWriter from blocking forever
                s.setSoLinger(true,1); // Probably doesn't help, but we do often close the socket immediately after sending data
                
                advertiseNeighbor(port, distance);
                
                BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream())); 
                PrintWriter pw = new PrintWriter(s.getOutputStream(),true);
                pw.println("H " + portNumber);  // Identify ourselves to the remote port
                pw.flush();
                pw.printf("l %d %d %d", lsp.senderPort,lsp.seq,lsp.ttl);
                if (lsp.receiverFromPort != portNumber)  // Do not send this where we received it from
                    for (int x=0; x < lsp.adjRouterPort.size(); x++) {
                        pw.printf(" %d-%d",lsp.adjRouterPort.get(x),lsp.distance.get(x));        
                     }
                pw.println();
                pw.flush();
         
                if("ACK".equals(br.readLine())) {
                    sr.seqAck().put(lsp.senderPort,lsp.seq);
                }
                
               
                s.close();
                
              }
        }

        private void advertiseNeighbor(int port, int distance) {
            // We are connectd to the remote router, make sure we are advertising that we can connect to it
            if (!myLSA.adjRouterPort.contains(port)) {
                myLSA.adjRouterPort.add(port);
                myLSA.distance.add(distance);
                sendRefresh(myLSA); // Send a refresh LSP to all neighbors
            }
        }

        private void dropNeighborFromAdj(int port) {
            // Socket is closed/can't be opened.  Remove from list of adjacents
            List<Integer> listAdjacentsCopy = new ArrayList<Integer>(myLSA.adjRouterPort); // iterate over a copy because concurrency
            for (Integer a : listAdjacentsCopy) {
                if (a == port) {
                    myLSA.distance.remove(myLSA.adjRouterPort.indexOf(a));
                    myLSA.adjRouterPort.remove(myLSA.adjRouterPort.indexOf(a));
                    sendRefresh(myLSA); // Send a refresh LSP to all neighbors
                }
            }
        }

        private void cleanLSPTable() {
            List<LSP> listLSPcopy = new ArrayList<LSP>(listLSP); // iterate over a copy because concurrency
            for (LSP lsp : listLSPcopy) {
                if (lsp.ttl == 0) listLSP.remove(lsp);   
            }
        }

        private void recalculateTableIfStable() {
            if ( (stableTime > STABLE_TIME) && routeTableRecalcNeeded)  {
                routeTable = new routingTable(listLSP, portNumber);
                routeTable.calculateTable();
                routeTableRecalcNeeded=false; //we don't need to do it again until we get a new LSP
            }
        }
    }
private final class ServerThread implements Runnable {
        private final int portNumber;
        ServerSocket ss;
        Socket s;

        private ServerThread(int portNumber) {
            this.portNumber = portNumber;
        }

        public void run()  { 
            Thread.currentThread().setName("[" + Integer.toString(portNumber) + "] Listener");

            try {
                 ss = new ServerSocket(portNumber);
            }
            catch (Exception IOException) {
                System.out.printf("Error in binding port %d\n",portNumber);
                return;
            } 
           
            while (keepRunning) {
                try {
                    s = ss.accept();
                    ExecutorService acceptor = Executors.newSingleThreadExecutor();  
                    acceptor.submit(() -> acceptConnection(s));
                    
                }
                catch (Exception IOException) {
                    System.out.printf("[%d] error on accept() - %s\n",portNumber,IOException);
                }
        
            }
            try {
                ss.close();
            } catch (Exception IOException) {
                System.out.printf("[%d] error on ss.close() - %s\n",portNumber,IOException);

            }
        }
        private void acceptConnection(Socket s) { // This code is used by the listener thread to accept a connection and handle it
            int remotePort=-1;
            Thread.currentThread().setName("[" + Integer.toString(portNumber) + "] acceptConnection");
            try { 
                BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream())); 
                PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
                String command = br.readLine();
                /* MockRouter should be able to respond to three possible messages: 
                 * (a) link state messages to which it responds with ACK and a newline, 
                 * (b) h followed by a newline (a history message) to which it responds with all the link state messages its received (and at what time since the start of the simulation), 
                 * followed by a line with the word TABLE followed by lines giving its routing table, 
                 * (c) s followed by a newline (stop) to which it responds with STOPPING and a newline and then it stops its thread. 
                */
                if (command.charAt(0) == CMD_HELLO) {  // Received Hello (format is H REMOTEPORT), then read the next line
                    String[] chunks = command.split(" ");
                    remotePort = Integer.parseInt(chunks[1]);
                    command = br.readLine();
                }
                switch(command.charAt(0)) {
                    case CMD_KEEPALIVE:
                        handleKeepalive(pw);  
                        break;
                    case CMD_LSP:
                        receiveLSP(remotePort, pw, command);
                        break;
                    case CMD_HISTORY:
                        printTable(pw);
                        break;
                    case CMD_STOP:
                        handleStop(s, pw);
                        break;
                    default:
                        pw.println("Unknown command: " + command);
                        pw.flush();
                }
                
                        
                if (command.charAt(0) == CMD_KEEPALIVE) {  // Keepalive.  Just just a do-nothing to verify the connect/accept/read/write was working, sends a k back.
                    handleKeepalive(pw);  
                }    
                if (command.charAt(0) == CMD_LSP) {  
                    receiveLSP(remotePort, pw, command);
            
                }
                if (command.charAt(0) == CMD_HISTORY) {     
                    printTable(pw);
                }
                if (command.charAt(0) == CMD_STOP) {        
                    handleStop(s, pw);
                }
            
            
            
            }
            catch(Exception ignore) {
                //System.out.printf("[%d] Error1: %s\n",portNumber,ignore);
            }
            try {
                s.close();
            } catch (Exception ignore) {
                //System.out.printf("[%d] Error2: %s\n",portNumber,ignore);
            }
        }
        private void handleKeepalive(PrintWriter pw) {
            pw.println("k");
            pw.flush();
        }
}
private class LsaRefresh implements Runnable {
    @Override
    public void run() {
      Thread.currentThread().setName("[" + Integer.toString(portNumber) + "] lsaRefresh");
      sendRefresh(myLSA); 
    }
}
private class TtlAger implements Runnable {
        public void run() {
            Thread.currentThread().setName("[" + Integer.toString(portNumber) + "] TTL Ager");
            try {
                // These 2 lines code keep Stabletime updated and print it out.  This isn't really needed, I just wanted to see it.  This should be moved to the section of code that has to recalculate the routing table.
                stableTime = Instant.now().toEpochMilli() - timeOfLastLSP;
                List<LSP> listLSPcopy = new ArrayList<>(listLSP); // Because we are removing items from the list, we need to make a copy of it to iterate over
                for (LSP lspdb : listLSPcopy) {
                    int ttl = lspdb.ttl;      
                    if (ttl >= 1) lspdb.ttl--;  
                    if (ttl==0) routeTableRecalcNeeded = true;
                }
            } 
            catch (Exception ignore) {
                //System.out.printf("[%d] Error3: %s\n",portNumber,ignore);

            }
        }    
    }
    
}
