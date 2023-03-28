/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package linkstate;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author zayd
 */
public class LinkStateSimulator {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        // TODO code application logic here
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
                MockRouter router = new MockRouter(Integer.getInteger(temp[0]),temp2);
                routers.add(router);
                new Thread(router.Listener).start();
                new Thread(router.Initiator).start();
                s="";
            }
            c = (char) f.read();
        }
        
        System.out.println("Initialization complete. Ready to accept commands.");
        
//        Scanner input = new Scanner(System.in);
//        String in = input.next();
//        while(in != "e"){
//            String[] cm = in.split(" ");
//            if(cm[0] == "h"){
//                
//            }
//            if(cm[0] == "s"){
//                
//            }
//            in = input.next();
//        }
    }
    
}
