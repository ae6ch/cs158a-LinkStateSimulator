//Zayd Kudaimi 015637245 Shinhyung Lee 014175837 Steve Rubin 017439448

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class LinkStateSimulator {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        ArrayList routers = new ArrayList<MockRouter>();
        File topology = new File(args[0]);
        FileReader f = new FileReader(topology);
        char c = (char) f.read();
        String s = "";
        while(c != (char)-1){
            s += c;
            if(c == '\n'){
                s = s.strip();
                String[] temp = s.split(" ");
                String[] temp2 = new String[temp.length-1];
                for(int i = 0; i < temp2.length; i++){
                    temp2[i] = temp[i+1];
                }
                MockRouter router = new MockRouter(Integer.parseInt(temp[0]),temp2);
                routers.add(router);
                new Thread(router.Listener).start();
                new Thread(router.Initiator).start();
                s="";
            }
            c = (char) f.read();
        }
        
        System.out.println("Initialization complete. Ready to accept commands.");
        
        Scanner input = new Scanner(System.in);
        while(input.hasNext()) {
            String in = input.nextLine();

            String[] cm = in.split(" ");
            switch (cm[0]) {
                case "e": {
                    System.out.println("Goodbye!");
                    System.exit(0);
                }
                case "s":
                case "h":
                    sendCommand("localhost",Integer.parseInt(cm[1]),cm[0]);
                    break;
            }  
        }
    }
    private static void sendCommand(String host, int port, String command)  {
        Socket socket;
        try {
            socket = new Socket(host, port);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println(command);
            out.flush();
            String buf;
            while((buf=br.readLine()) != null)
                System.out.println(buf);
            socket.close();
        } catch (Exception e) {
            System.out.println("error: " + e);
        }
    }
}

