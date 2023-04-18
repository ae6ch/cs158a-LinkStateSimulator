//Zayd Kudaimi  Shinhyung Lee  Steve Rubin 

public class MockRouterTest {
    public static void main(String [] args) {
        MockRouter mr1 = new MockRouter(9990,"10000-1 9991-1".split(" "));
        MockRouter mr2 = new MockRouter(9991,"9990-1 9992-1".split(" "));
        MockRouter mr3 = new MockRouter(9992,"9991-1 9993-1 9995-1".split(" "));
        MockRouter mr4 = new MockRouter(9993,"9992-1 9994-1 9997-5".split(" "));
        MockRouter mr5 = new MockRouter(9994,"9993-1 9995-1".split(" "));
        MockRouter mr6 = new MockRouter(9995,"9994-1 9996-1 9992-1".split(" "));
        MockRouter mr7 = new MockRouter(9996,"9995-1 9997-1".split(" "));
        MockRouter mr8 = new MockRouter(9997,"9996-1 9998-1 9993-5".split(" "));
        MockRouter mr9 = new MockRouter(9998,"9997-1 9999-1".split(" "));
        MockRouter mr10 = new MockRouter(9999,"9998-1 10000-1".split(" "));
        MockRouter mr11 = new MockRouter(10000,"9999-1 9990-1".split(" "));

        // Start all the server(listener) threads
        new Thread(mr1.Listener).start();
        new Thread(mr2.Listener).start();
        new Thread(mr3.Listener).start(); 
        new Thread(mr4.Listener).start();
        new Thread(mr5.Listener).start();
        new Thread(mr6.Listener).start();
        new Thread(mr7.Listener).start();
        new Thread(mr8.Listener).start();
        new Thread(mr9.Listener).start();
        new Thread(mr10.Listener).start();
        new Thread(mr11.Listener).start();


        // Client not implemented yet
        // Start all the client(initiator) threads
        new Thread(mr1.Initiator).start();
        new Thread(mr2.Initiator).start();
        new Thread(mr3.Initiator).start();
        new Thread(mr4.Initiator).start();
        new Thread(mr5.Initiator).start();
        new Thread(mr6.Initiator).start();
        new Thread(mr7.Initiator).start();
        new Thread(mr8.Initiator).start();
        new Thread(mr9.Initiator).start();
        new Thread(mr10.Initiator).start();
        new Thread(mr11.Initiator).start();

       



    }
}
