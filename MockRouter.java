//Zayd Kudaimi 015637245 Shinhyung Lee 014175837 Steve Rubin 017439448


import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.concurrent.*;

import javax.swing.text.rtf.RTFEditorKit;

import java.util.*;
import java.time.*;

/**
 *
 * @author zayd
 */
public class MockRouter  {
    public final Runnable Listener;
    public final Runnable Initiator;
    int portNumber;  
    String[] adjacents;
    boolean keepRunning=true;
    long startTime = Instant.now().toEpochMilli();
    int nextSeq = 1; // The next sequence number we use;
    final int INITIAL_SEQ = 0; // The initial TTL for a LSP
    final int INITIAL_TTL = 60; // The initial TTL for a LSP
    final int LSA_REFRESH_TIME = 30; // The time between sending our own LSP announcements
    final int STABLE_TIME = 5000; // How long we have to be stable before we can calculate the routing table
    boolean routeTableRecalcNeeded = false; // Do we need to recalculate the routing table? (set to true every time we get a new LSP)
    LinkedList<LSP> listLSP = new LinkedList<LSP>();
    LinkedList<LSP> listLSPHistory = new LinkedList<LSP>();
    long timeOfLastLSP=Instant.now().toEpochMilli();  // Last time we received a LSP packet, used to calculate if LSPs are stable
    long stableTime=0; // How long have we been stable? Updated by the TTL expire thread for now, need to move to the watchdog thread for calculating the routing table 
    routingTable routeTable = new routingTable(listLSP,portNumber);


    // Start the background processes
    ScheduledExecutorService ttlAger = Executors.newSingleThreadScheduledExecutor();  // This is the TTL Age task
    ScheduledExecutorService lsaRefresh = Executors.newSingleThreadScheduledExecutor();  // This is the lsaRefresh task
private  void sendRefresh(LSP myLSA){
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

private String lsaListPW(List <LSP> lspList) {
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

private void acceptConnection(Socket s) { // This code is used by the listener thread to accept a connection and handle it
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
        if (command.charAt(0) == 'k') {  // Keepalive.  Just just a do-nothing to verify the connect/accept/read/write was working, sends a k back.
            pw.println("k");
            pw.flush();  
        }    
        // Stores the links-distance pairs into 2 arrays, and then puts those in a record class called LSP, those LSPs are then put in a list called listLSP;
        if (command.charAt(0) == 'l') {  // Received a link state message
            String chunks[] = command.split(" ");
            boolean dupSeq = false;
            ArrayList<Integer> adjRouterPorts = new ArrayList<Integer>(100);
            ArrayList<Integer> distances = new ArrayList<Integer>(100);
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
            //if (dupSeq==false) { 
                timeOfLastLSP = Instant.now().toEpochMilli();
                routeTableRecalcNeeded = true;
                LSP lsp = new LSP(timeReceived,Integer.parseInt(chunks[1]),Integer.parseInt(chunks[2]),Integer.parseInt(chunks[3]),adjRouterPorts,distances);
                listLSP.add(lsp);
            } 
            
            pw.println("ACK");
            pw.flush();
    
        }
    
        if (command.charAt(0) == 'h') {           // Dump all link state messages and routing table *TODO*
            pw.printf("HISTORY\n----------------------------------------\n%s\n",lsaListPW(listLSPHistory));
            pw.flush();

            pw.printf("CURRENT\n----------------------------------------\n%s\n",lsaListPW(listLSP));
            pw.flush();

            pw.printf("TABLE\n---------------------------------------\n%s\n",routeTable.printTable());
            pw.flush();
        
    
        }
        if (command.charAt(0) == 's') {           // STOP *DONE?*
            //System.out.printf("Port %d:%s:%d - RX STOP\n",portNumber,s.getInetAddress().getHostAddress(),s.getPort());                       
            pw.println("STOPPING");
            pw.flush();
            s.close();
            keepRunning=false;
            ttlAger.shutdown();
            lsaRefresh.shutdown();
            Thread.currentThread().interrupt();
           

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
    public MockRouter(int portNumber, String[] adjacents)  { // adjacents is port-distance port-distace port-distace 
        this.adjacents = adjacents;
        this.portNumber = portNumber;
        
        // Initialize the LSP for this router, we will add to the myAdjRouterPorts and myDistances as sockets come up
        ArrayList<Integer> myAdjRouterPorts = new ArrayList<Integer>(100);
        ArrayList<Integer> myDistances = new ArrayList<Integer>(100);
        LSP myLSA = new LSP(Instant.now().toEpochMilli()-startTime, portNumber, INITIAL_SEQ,INITIAL_TTL, myAdjRouterPorts, myDistances);


        // Insert a LSP for all of our current links at start up
        //for (int x=0; x < adjacents.length; x++) {
        //    String[] myAdjRouterPortDistance = adjacents[x].split("-");
        //    myAdjRouterPorts.add(Integer.parseInt(myAdjRouterPortDistance[0]));
        //   myDistances.add(Integer.parseInt(myAdjRouterPortDistance[1]));
         //  }   
       
             
        lsaRefresh.scheduleAtFixedRate(new Runnable() {
          @Override
          public void run() {
            Thread.currentThread().setName("[" + Integer.toString(portNumber) + "] lsaRefresh");

            //System.out.printf("[%d] LSA Refresh\n",portNumber);
            sendRefresh(myLSA);
            
            
          }
        }, 0, LSA_REFRESH_TIME, TimeUnit.SECONDS);
  


        ttlAger.scheduleAtFixedRate(new Runnable() {
          @Override
          
          public void run() {
            Thread.currentThread().setName("[" + Integer.toString(portNumber) + "] TTL Ager");
            try {
                // These 2 lines code keep Stabletime updated and print it out.  This isn't really needed, I just wanted to see it.  This should be moved to the section of code that has to recalculate the routing table.
                stableTime = Instant.now().toEpochMilli() - timeOfLastLSP;
                //System.out.printf("[%d] Stabletime: %d routeTableRecalcNeeded: %b\n",portNumber,stableTime,routeTableRecalcNeeded);
                List<LSP> listLSPcopy = new ArrayList<LSP>(listLSP); // Because we are removing items from the list, we need to make a copy of it to iterate over
                for (LSP lspdb : listLSPcopy) {
                  int ttl = lspdb.ttl;
                 if (ttl == 1) {  // It's going to expire this time around
                    //System.out.printf("[%d] TTL expired for %d\n",portNumber,lspdb.senderPort);
                    routeTableRecalcNeeded = true;

                 }
                 if (ttl <= 0) { // It's expired or already expired
                    //We don't do anything here.  The 0 TTL LSPs site in the LSP list, the initiator thread will remove them after it refloods.
                    //listLSP.remove(lspdb); // Remove it from the original list
                    //routeTableRecalcNeeded = true;
                 } 
                 else {
                     lspdb.ttl--;                
                 }
             }
            } 
            catch (Exception ignore) {
                //System.out.printf("[%d] Error3: %s\n",portNumber,ignore);

            }
          }
        }, 0, 1, TimeUnit.SECONDS);


        Listener = new Runnable() {           // SERVER CODE GOES HERE
            ServerSocket ss;
            Socket s;
            public void run()  { 
                Thread.currentThread().setName("[" + Integer.toString(portNumber) + "] Listener");

                try {
                     ss = new ServerSocket(portNumber);
                     ss.setSoTimeout(10000); // Try to prevent BufferedReader and PrintWriter from blocking forever

                }
                catch (Exception IOException) {
                    System.out.printf("Error in binding port %d\n",portNumber);
                    return;
                } 
               
                while (keepRunning) {
                    try {
                        try {
                            s = ss.accept();
                            acceptConnection(s);  // BUGGY: If you want concurrency, you need to spawn a thread here, comment out this line and uncomment the ExecutorService code below

                        }
                        catch (SocketTimeoutException ignore) {} // We set a timeout, so this is expected

                        /* 
                        ExecutorService acceptor = Executors.newSingleThreadExecutor();  // The code does not handle concurrency very well, so commented this out
                        acceptor.submit(new Runnable() {
                            @Override
                            public void run() {
                                acceptConnection(s);
                            }
                        });
                        */
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

        };

        Initiator = new Runnable() {                
            public void run() {             // CLIENT CODE GOES HERE
                int port;
                int distance;
                
                Thread.currentThread().setName("[" + Integer.toString(portNumber) + "] Initiator");

                HashMap<Integer,seqRecord> hashseqRecord = new HashMap<>();

                
                while (keepRunning) { // Recalculate the routing table if we have been stable for a while
                    if ( (stableTime > STABLE_TIME) && routeTableRecalcNeeded)  {
                        //System.out.printf("[%d] STABLE TIME EXCEEDED, RECALCULATING ROUTING TABLE\n",portNumber);
                        // KICK OFF THE DIKJSTRA ALGORITHM HERE
                        routeTable = new routingTable(listLSP, portNumber);
                        routeTable.calculateTable();
                        
                        //System.out.printf("[%d] Routing Table update complete\n",portNumber);
                        routeTableRecalcNeeded=false; //we don't need to do it again until we get a new LSP
                    }

                    float sleepyTime=(float) ((Math.random()*1000)+3000);

                    try { 
                        //System.out.printf("[%d] Client sleeping for %f\n",portNumber,sleepyTime);
                        Thread.sleep((long)sleepyTime);
                    } catch (Exception ignore) {};
                    String[] endpoints = adjacents;
                    for (String e: endpoints) {     // Main outgoing connection loop here
                        String ep[] = e.split("-");
                        port = Integer.parseInt(ep[0]);
                        distance = Integer.parseInt(ep[1]);
                        // Find seqRecord for this ep, if it doesnt exist, insert a new one with a empty seqAck table
                        if (!hashseqRecord.containsKey(Integer.parseInt(ep[0]))) {
                            HashMap<Integer,Integer> sa = new HashMap<Integer,Integer>();
                            seqRecord sr = new seqRecord(Integer.parseInt(ep[0]),sa);
                            hashseqRecord.put(Integer.parseInt(ep[0]),sr);
                        }
                        seqRecord sr = hashseqRecord.get(Integer.parseInt(ep[0]));
                        //sr.seqAck().get(Integer.parseInt(ep[0]));
                        try {
                         
                          // loop through lsp list, if there is a lsp, and the seq number is greater than the seqAck then send it
                          List<LSP> listLSPcopy = new ArrayList<LSP>(listLSP); // iterate over a copy because concurrency 
                          for (LSP lsp : listLSPcopy) {
                            // If we dont have a seqAck for this senderPort, then add it
                            if (!sr.seqAck().containsKey(lsp.senderPort))
                                sr.seqAck().put(lsp.senderPort,-1 );
                          
                            if ((sr.seqAck().get(lsp.senderPort) >= lsp.seq) && (lsp.ttl > 0)) { // We've already send this LSP and its not expired
                                                                                                 // if lsp.ttl >0 is how we reflood a 0 TTL LSP
                                //System.out.printf("[%d->%s] Skipping LSP %d:%d\n",portNumber,ep[0],lsp.senderPort,lsp.seq);
                                continue;
                            }
                            // Send the LSP
                            //System.out.printf("[%d->%s]  Sending LSP %d:%d\n",portNumber,ep[0],lsp.senderPort,lsp.seq);
                            Socket s = new Socket("localhost", port);
                            s.setSoTimeout(10000); // Try to prevent BufferedReader and PrintWriter from blocking forever
                            s.setSoLinger(true,1); // Probably doesn't help, but we do often close the socket immediately after sending data
                            
                            // We are connectd to the remote router, make sure we are advertising that we can connect to it
                            if (!myLSA.adjRouterPort.contains(port)) {
                                myLSA.adjRouterPort.add(port);
                                myLSA.distance.add(distance);
                                sendRefresh(myLSA); // Send a refresh LSP to all neighbors
                            }
                            
                        

                            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream())); 
                            PrintWriter pw = new PrintWriter(s.getOutputStream(),true);
                            pw.printf("l %d %d %d", lsp.senderPort,lsp.seq,lsp.ttl);
                            for (int x=0; x < lsp.adjRouterPort.size(); x++) {
                                pw.printf(" %d-%d",lsp.adjRouterPort.get(x),lsp.distance.get(x));
                                
                            }
                            pw.println();
                            pw.flush();
               
                            if("ACK".equals(br.readLine())) {
                                //System.out.printf("[%d->%s]  Received ACK for LSP %d:%d\n",portNumber,ep[0],lsp.senderPort,lsp.seq);
                                sr.seqAck().put(lsp.senderPort,lsp.seq);
                            }
                            else {
                                //System.out.printf("[%d->%s]  No ACK for LSP %d:%d\n",portNumber,ep[0],lsp.senderPort,lsp.seq);
                            }
                           
                            s.close();
                            
                          }
                        } 
                        catch (ConnectException ce) {
                            // Socket is closed/can't be opened.  Remove from list of adjacents
                            //System.out.printf("[%d] Error1: %s\n",portNumber,e);
                            List<Integer> listAdjacentsCopy = new ArrayList<Integer>(myLSA.adjRouterPort); // iterate over a copy because concurrency
                            for (Integer a : listAdjacentsCopy) {
                                if (a == port) {
                                    //System.out.printf("[%d] Removing %d from adjacents\n",portNumber,port);
                                    myLSA.distance.remove(myLSA.adjRouterPort.indexOf(a));
                                    myLSA.adjRouterPort.remove(myLSA.adjRouterPort.indexOf(a));
                                    sendRefresh(myLSA); // Send a refresh LSP to all neighbors


                                }
                            }
                        }
                        catch (Exception ignore) {
                            //System.out.printf("[%d] Error4: %s\n",portNumber,ignore);
                        } 
                    }           
                
                    // We've flooded all the expired LSPs at this point, Delete any LSP that has a TTL of 0
                    List<LSP> listLSPcopy = new ArrayList<LSP>(listLSP); // iterate over a copy because concurrency
                    for (LSP lsp : listLSPcopy) {
                        if (lsp.ttl == 0) {
                            //System.out.printf("[%d] Deleting LSP after flood\n",portNumber);                     
                            listLSP.remove(lsp);
                        }
                    }
                }


            }
        };
        
    }

}
