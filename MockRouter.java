import java.net.*;
import java.util.regex.Pattern;
import java.io.*;

public class MockRouter {
    public final Runnable Listener;
    public final Runnable Initiator;  
    String adjacents;
    public MockRouter(int portNumber, String adjacents)  { // adjacents is port-distance port-distace port-distace 
        this.adjacents = adjacents;
        System.out.println("In the construtor");




        Listener = new Runnable() {           // SERVER CODE GOES HERE
            ServerSocket ss;
            Socket s;
            boolean keepRunning=true;
            public void run()  { 
                try {
                     ss = new ServerSocket(portNumber);
                }
                catch (Exception IOException) {
                    System.out.printf("Error in binding port %d\n",portNumber);
                    return;
                }
                while (keepRunning) {
                    System.out.printf("Port %d waiting on accept()\n",portNumber);
                    try {
                        s = ss.accept();
                    }
                    catch (Exception IOException) {
                    }
                    System.out.printf("Port %d accepted connection from %s:%d\n",portNumber,s.getInetAddress().getHostAddress(),s.getPort());
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
                            System.out.printf("Port %d:%s:%d - RX Keepalive\n",portNumber,s.getInetAddress().getHostAddress(),s.getPort());      
                            pw.println("k");
                            pw.flush();  
                        }    
                        if (command.charAt(0) == 'l') {  // Received a link state message *TODO*
                            System.out.printf("Port %d:%s:%d - RX Link State Message\n",portNumber,s.getInetAddress().getHostAddress(),s.getPort());        
                        }    
                        if (command.charAt(0) == 'h') {           // Dump all link state messages and routing table *TODO*
                            System.out.printf("Port %d:%s:%d - RX HISTORY\n",portNumber,s.getInetAddress().getHostAddress(),s.getPort());                       
                        }
                        if (command.charAt(0) == 's') {           // STOP *DONE?*
                            System.out.printf("Port %d:%s:%d - RX STOP\n",portNumber,s.getInetAddress().getHostAddress(),s.getPort());                       
                            pw.println("STOPPING");
                            pw.flush();
                            s.close();
                            keepRunning=false;
                            Thread.currentThread().interrupt();
                        }
             

                      }
                    catch (Exception IOException) {
                        System.out.printf("Error on Read\n");

                    }
                    try {
                        s.close();
                    } 
                    catch (Exception IOException) {
                        System.out.printf("Error closing socket\n");
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

                while (true) {
                    float sleepyTime=(float) ((Math.random()*1000)+3000.0);

                    try { 
                        System.out.printf("Client sleeping for %f\n",sleepyTime);
                        Thread.sleep(5000);
                    } catch (Exception ignore) {};
                    String endpoints[] = adjacents.split(" ");
                    for (String e: endpoints) {     // Main outgoing connection loop here
                     String ep[] = e.split("-");
                     System.out.printf("Thread %d connecting to Thread %s and Distance %s\n", portNumber,ep[0],ep[1]);
                     port = Integer.parseInt(ep[0]);
                     distance = Integer.parseInt(ep[1]);
                        try {
                          Socket s = new Socket("localhost", port);
                           BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream())); 
                           PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
                           // do some stuff here with the remote connection
                           pw.println("k"); // just send a keepalive for now, needs to come out when we do the link state packet part
                           pw.flush();
                                if (br.readLine().charAt(0) == 'k') { 
                                    System.out.printf("Received Keepalive back from %d\n",port);
                                }
                                else {
                                    System.out.printf("");
                                }
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