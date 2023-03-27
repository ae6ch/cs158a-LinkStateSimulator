package linkstate;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

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
        File topology = new File(args[0]);
        FileReader f = new FileReader(topology);
        char c = (char) f.read();
        String s = "";
        while(c != (char)-1){
            s += c;
            c = (char) f.read();
        }
    }
    
}
