/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
//package linkstate;

import java.net.*;
import java.util.regex.Pattern;
import java.io.*;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.concurrent.*;


import java.util.*;
import java.time.*;

/**
 *
 * @author zayd
 */
public class MockRouter {
    public final Runnable Listener;
    public final Runnable Initiator;  
    String[] adjacents;
    boolean keepRunning=true;
    long startTime = Instant.now().toEpochMilli();
    int nextSeq = 1; // The next sequence number we use;
    final int INITIAL_SEQ = 0; // The initial TTL for a LSP
    final int INITIAL_TTL = 60; // The initial TTL for a LSP
    final int LSA_REFRESH_TIME = 30; // The time between sending our own LSP announcements
    LinkedList<LSP> listLSP = new LinkedList<LSP>();    
    
private void acceptConnection(Socket s) {                           // This code is used by the listener thread to accept a connection and handle it
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
            //System.out.printf("[%d] %s:%d - RX Keepalive\n",portNumber,s.getInetAddress().getHostAddress(),s.getPort());      
            pw.println("k");
            pw.flush();  
        }    
        // l sender_port seq_number time_to_live adjacent_router_1_port-distance_1 ... adjacent_router_n_port-distance_n
        // Stores the links-distance pairs into 2 arrays, and then puts those in a record class called LSP, those LSPs are then put in a list called listLSP;
        if (command.charAt(0) == 'l') {  // Received a link state message
            String chunks[] = command.split(" ");
            boolean dupSeq = false;
            ArrayList<Integer> adjRouterPorts = new ArrayList<Integer>(100);
            ArrayList<Integer> distances = new ArrayList<Integer>(100);
    
            for (int x=4; x < chunks.length; x++) {
                String[] adjRouterPortDistance = chunks[x].split("-");
               // System.out.printf("%s %s %s %s\n",chunks[0],chunks[1],chunks[2],chunks[3]);
               // System.out.printf("%s - %s\n",adjRouterPortDistance[0],adjRouterPortDistance[1]);
                adjRouterPorts.add(Integer.parseInt(adjRouterPortDistance[0]));
                distances.add(Integer.parseInt(adjRouterPortDistance[1]));
            }   
            for (LSP lspdb : listLSP) {
            // Find existing LSP for router
                if (lspdb.senderPort == Integer.parseInt(chunks[1])) {
                    if (lspdb.seq == Integer.parseInt(chunks[2])) { // If the sequence number is the same, igore it
                        dupSeq = true;
                        continue;
                    } 
                    if ( (lspdb.seq < Integer.parseInt(chunks[2])) && Integer.parseInt(chunks[3]) > 0) { // If the sequence number is newer and ttl>0, update it 
                        lspdb.time=Instant.now().toEpochMilli()-startTime;
                        lspdb.seq=Integer.parseInt(chunks[2]);
                        lspdb.ttl=Integer.parseInt(chunks[3]);
                        lspdb.adjRouterPort=adjRouterPorts;
                        lspdb.distance=distances;
                        dupSeq = true;
                        continue;
                    } 
                    if ( (lspdb.seq == Integer.parseInt(chunks[2])) && Integer.parseInt(chunks[3]) <= 0) { // If the sequence number is the same and ttl<=0, delete it
                        dupSeq = true;
                        listLSP.remove(listLSP.indexOf(lspdb));
                        continue;
                    } 
                    if (lspdb.seq > Integer.parseInt(chunks[2])) {   // If the sequence number is older, ignore it
                        dupSeq = true;
                        continue;
                    }
                }
            }
    
    
            int ttl = Integer.parseInt(chunks[3]);
            if (ttl >= 0 && dupSeq == false) { 
                LSP lsp = new LSP(Instant.now().toEpochMilli()-startTime,Integer.parseInt(chunks[1]),Integer.parseInt(chunks[2]),Integer.parseInt(chunks[3]),adjRouterPorts,distances);
                listLSP.add(lsp);
            } 
            else { 
    
           
            }
            pw.println("ACK");
            pw.flush();
    
        }
    
        if (command.charAt(0) == 'h') {           // Dump all link state messages and routing table *TODO*
            //System.out.printf("Port %d:%s:%d - RX HISTORY\n",portNumber,s.getInetAddress().getHostAddress(),s.getPort());  
            for (LSP lsp : listLSP) {
                pw.printf("T+%s %d %d %d", Long.toUnsignedString(lsp.time),lsp.senderPort,lsp.seq,lsp.ttl);
                for (int x=0; x < lsp.adjRouterPort.size(); x++) {
                    //pw.printf(" %d-%d",lsp.adjRouterPort()[x],lsp.distance()[x]);
                    pw.printf(" %d-%d",lsp.adjRouterPort.get(x),lsp.distance.get(x));
                }
                pw.println();
                pw.flush();
            }
    
        }
        if (command.charAt(0) == 's') {           // STOP *DONE?*
            //System.out.printf("Port %d:%s:%d - RX STOP\n",portNumber,s.getInetAddress().getHostAddress(),s.getPort());                       
            pw.println("STOPPING");
            pw.flush();
            s.close();
            keepRunning=false;
            Thread.currentThread().interrupt();
        }
    
    
    
    }
    catch(IOException e) {
        System.out.println("Error: " + e);
    }
    try {
        s.close();
    } catch (IOException e) {
        System.out.println("Error: " + e);
    }
}
    public MockRouter(int portNumber, String[] adjacents)  { // adjacents is port-distance port-distace port-distace 
        this.adjacents = adjacents;
        //System.out.println("In the construtor");

        
        ArrayList<Integer> myAdjRouterPorts = new ArrayList<Integer>(100);
        ArrayList<Integer> myDistances = new ArrayList<Integer>(100);
        LSP myLSA = new LSP(Instant.now().toEpochMilli()-startTime, portNumber, INITIAL_SEQ,INITIAL_TTL, myAdjRouterPorts, myDistances);

        // Insert a LSP for all of our current links at start up
        for (int x=0; x < adjacents.length; x++) {
            String[] myAdjRouterPortDistance = adjacents[x].split("-");
             myAdjRouterPorts.add(Integer.parseInt(myAdjRouterPortDistance[0]));
            myDistances.add(Integer.parseInt(myAdjRouterPortDistance[1]));
         }   
        // need to do this more then once, its going to age out eventually?
        //listLSP.add(new LSP(Instant.now().toEpochMilli()-startTime, portNumber, nextSeq++,INITIAL_TTL, myAdjRouterPorts, myDistances));
        
             
        ScheduledExecutorService lsaRefresh = Executors.newSingleThreadScheduledExecutor();  // This is the lsaRefresh task
        lsaRefresh.scheduleAtFixedRate(new Runnable() {
          @Override
          public void run() {
            //System.out.printf("LSA Refresh for %d\n",portNumber);
            myLSA.time=Instant.now().toEpochMilli()-startTime;
            myLSA.seq++;
            myLSA.ttl=INITIAL_TTL;

            if(listLSP.contains(myLSA)) { 
                listLSP.set(listLSP.indexOf(myLSA), myLSA);
            }
            else {
                listLSP.add(myLSA);
            }
          }
        }, 0, LSA_REFRESH_TIME, TimeUnit.SECONDS);
  


        ScheduledExecutorService ttlAger = Executors.newSingleThreadScheduledExecutor();  // This is the TTL Age task
        ttlAger.scheduleAtFixedRate(new Runnable() {
          @Override
          public void run() {
           
            for (LSP lspdb : listLSP) {
                int ttl = lspdb.ttl;
                if (ttl == 1) {  // It's going to expire this time around
                    System.out.printf("TTL expired for %d\n",lspdb.senderPort);
                }
                if (ttl <= 0) { // It's expired or already expired
                    // Probably need to populate a flood list per peer unfortunately
                    listLSP.remove(lspdb);
                } 
                else {
                    lspdb.ttl--;                
                   // listLSP.set(listLSP.indexOf(lspdb), new LSP(lspdb.time,lspdb.senderPort,lspdb.seq,ttl,lspdb.adjRouterPort,lspdb.distance));
                }
            }
          }
        }, 0, 1, TimeUnit.SECONDS);


        Listener = new Runnable() {           // SERVER CODE GOES HERE
            ServerSocket ss;
            Socket s;
            public void run()  { 
                try {
                     ss = new ServerSocket(portNumber);
                }
                catch (Exception IOException) {
                    System.out.printf("Error in binding port %d\n",portNumber);
                    return;
                }
                while (keepRunning) {
                    //System.out.printf("Port %d waiting on accept()\n",portNumber);
                    try {
                        s = ss.accept();
                        //acceptConnection(s);
                        //ScheduledExecutorService ttlAger = Executors.newSingleThreadScheduledExecutor()
                        ExecutorService acceptor = Executors.newSingleThreadExecutor();
                        acceptor.submit(new Runnable() {
                            @Override
                            public void run() {
                                acceptConnection(s);
                            }
                        });
                    }
                    catch (Exception IOException) {
                    }
            
                 }
                try {
                    ss.close();
                } catch (Exception IOException) {
                    System.out.printf("Port %d error on ss.close()\n",portNumber);

                }
            }

        };

        Initiator = new Runnable() {                
            public void run() {             // CLIENT CODE GOES HERE
                int port;
                int distance;
                
                Hashtable<Integer,seqRecord> hashseqRecord = new Hashtable<>();

                
                //Hashtable<Integer,Integer> seqAck = new Hashtable<Integer,Integer>();
                while (keepRunning) {
                    float sleepyTime=(float) ((Math.random()*1000)+3000);

                    try { 
                        //System.out.printf("Client sleeping for %f\n",sleepyTime);
                        Thread.sleep((long)sleepyTime);
                    } catch (Exception ignore) {};
                    String[] endpoints = adjacents;
                    for (String e: endpoints) {     // Main outgoing connection loop here
                        String ep[] = e.split("-");
                        //System.out.printf("[%d] connecting to Thread %s and Distance %s\n", portNumber,ep[0],ep[1]);
                        port = Integer.parseInt(ep[0]);
                        distance = Integer.parseInt(ep[1]);
                        // Find seqRecord for this ep, if it doesnt exist, insert a new one with a empty seqAck table
                        if (!hashseqRecord.containsKey(Integer.parseInt(ep[0]))) {
                            Hashtable<Integer,Integer> sa = new Hashtable<Integer,Integer>();
                            seqRecord sr = new seqRecord(Integer.parseInt(ep[0]),sa);
                            hashseqRecord.put(Integer.parseInt(ep[0]),sr);
                        }
                        seqRecord sr = hashseqRecord.get(Integer.parseInt(ep[0]));
                        //sr.seqAck().get(Integer.parseInt(ep[0]));
                        try {
                          Socket s = new Socket("localhost", port);
                           BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream())); 
                           PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
                           // do some stuff here with the remote connection
                          // loop through lsp list, if there is a lsp, and the seq number is greater than the seqAck then send it
                          for (LSP lsp : listLSP) {

                            //System.out.printf("[%d->%s] %s current lsp0 looking at %d:%d\n",portNumber,ep[0],sr.seqAck().get(lsp.senderPort),lsp.senderPort,lsp.seq);
                            //System.out.printf("[%d->%s] %s current lsp0 looking at %d\n",portNumber,ep[0],sr.seqAck.get(lsp.senderPort()),lsp.seq());
                            if (!sr.seqAck().containsKey(lsp.senderPort))
                                sr.seqAck().put(lsp.senderPort,-1 );
                          
                            if (sr.seqAck().get(lsp.senderPort) >= lsp.seq) {
                                //System.out.printf("[%d->%s] Skipping LSP %d:%d\n",portNumber,ep[0],lsp.senderPort,lsp.seq);
                                continue;
                            }
                            // Send the LSP
                            System.out.printf("[%d->%s]  Sending LSP %d:%d\n",portNumber,ep[0],lsp.senderPort,lsp.seq);
                            pw.printf("l %d %d %d", lsp.senderPort,lsp.seq,lsp.ttl);
                            for (int x=0; x < lsp.adjRouterPort.size(); x++) {
                                //pw.printf(" %d-%d",lsp.adjRouterPort()[x],lsp.distance()[x]);
                                pw.printf(" %d-%d",lsp.adjRouterPort.get(x),lsp.distance.get(x));
                                
                            }
                            sr.seqAck().put(lsp.senderPort,lsp.seq);  // add logic to make sure we received the ACK above?
                            pw.println();
                            pw.flush();
                            break; // we can only send one LSP per connection

                          }
                          //pw.println("k"); // just send a keepalive for now, needs to come out when we do the link state packet part
                          //pw.flush();
                          //  if (br.readLine().charAt(0) == 'k') { 
                          //     //System.out.printf("Received Keepalive back from %d\n",port);
                          //  }
                          //  else {
                          //      //System.out.printf("");
                          //  }
                          //  s.close();
                        } 
                       catch (Exception IOException) {
                          // error opening the socket
                        }
                    }           
                }

            }
        };
    }

}
