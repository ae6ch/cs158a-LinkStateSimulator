import java.net.*;
import java.io.*;

public class MockRouter {
    public final Runnable Listener;
    public final Runnable Initiator;  
    
    public MockRouter(int portNumber, String adjacents)  {
        System.out.println("In the construtor");
   
        Listener = new Runnable() {           // SERVER CODE GOES HERE
            boolean keepRunning=true;
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
                    System.out.printf("Port %d waiting on accept()\n",portNumber);
                    try {
                        s = ss.accept();
                    }
                    catch (Exception IOException) {
                    }
                    System.out.printf("Port %d accepted connection from %s:%d\n",portNumber,s.getInetAddress().getHostAddress(),s.getLocalPort());
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
                        if (command.charAt(0) == 'l') {  // Received a link state message *TODO*
                            System.out.printf("Port %d:%s:%d - RX Link State Message\n",portNumber,s.getInetAddress().getHostAddress(),s.getLocalPort());        
                        }    
                        if (command.charAt(0) == 'h') {           // Dump all link state messages and routing table *TODO*
                            System.out.printf("Port %d:%s:%d - RX HISTORY\n",portNumber,s.getInetAddress().getHostAddress(),s.getLocalPort());                       
                        }
                        if (command.charAt(0) == 's') {           // STOP *DONE?*
                            System.out.printf("Port %d:%s:%d - RX STOP\n",portNumber,s.getInetAddress().getHostAddress(),s.getLocalPort());                       
                            pw.println("STOPPING");
                            pw.flush();
                            s.close();
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

            }
        };
    }

}