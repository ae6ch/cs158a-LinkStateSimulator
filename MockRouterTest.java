// A test class for MockRouter.  Starts 3 MockRouters on ports 9990/9991/9992 connected in a line.  9990-9991-9992 all with distance of 1
public class MockRouterTest {
    public static void main(String args[]) {
        MockRouter mr1 = new MockRouter(9990,"9991-1 9992-6 9993-1 9994-4 9995-1 9996-2 9997-1".split(" "));
        MockRouter mr2 = new MockRouter(9991,"9990-1".split(" "));
        MockRouter mr3 = new MockRouter(9992,"9990-6".split(" "));
        
        MockRouter mr4 = new MockRouter(9993,"9990-1 9996-4".split(" "));
        MockRouter mr5 = new MockRouter(9994,"9990-4".split(" "));
        MockRouter mr6 = new MockRouter(9995,"9990-2".split(" "));
        MockRouter mr7 = new MockRouter(9996,"9990-1 9993-4".split(" "));

        // Start all the server(listener) threads
        new Thread(mr1.Listener).start();
        new Thread(mr2.Listener).start();
        new Thread(mr3.Listener).start();   
        new Thread(mr4.Listener).start();
        new Thread(mr5.Listener).start();
        new Thread(mr6.Listener).start();
        new Thread(mr7.Listener).start();
        // Client not implemented yet
        // Start all the client(initiator) threads
        new Thread(mr1.Initiator).start();
        new Thread(mr2.Initiator).start();
        new Thread(mr3.Initiator).start();
        new Thread(mr4.Initiator).start();
        new Thread(mr5.Initiator).start();
        new Thread(mr6.Initiator).start();
        new Thread(mr7.Initiator).start();
       
       



    }
}
