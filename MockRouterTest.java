// A test class for MockRouter.  Starts 3 MockRouters on ports 9990/9991/9992 connected in a line.  9990-9991-9992 all with distance of 1
public class MockRouterTest {
    public static void main(String args[]) {
        MockRouter mr1 = new MockRouter(9990,"9991-1".split(" "));
        MockRouter mr2 = new MockRouter(9991,"9990-1 9992-1".split(" "));
        MockRouter mr3 = new MockRouter(9992,"9991-1".split(" "));


        // Start all the server(listener) threads
        new Thread(mr1.Listener).start();
        new Thread(mr2.Listener).start();
        new Thread(mr3.Listener).start();

        // Client not implemented yet
        // Start all the client(initiator) threads
        new Thread(mr1.Initiator).start();
        new Thread(mr2.Initiator).start();
        new Thread(mr3.Initiator).start();



    }
}
