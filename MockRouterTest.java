public class MockRouterTest {
    public static void main(String [] args) {
        MockRouter mr1 = new MockRouter(9990,"9992-6 9993-1 9996-2".split(" "));
        MockRouter mr2 = new MockRouter(9991,"9993-10 9994-12".split(" "));
        MockRouter mr3 = new MockRouter(9992,"9990-6 9997-3 10000-4".split(" "));
        MockRouter mr4 = new MockRouter(9993,"9990-1 9996-4 9991-10".split(" "));
        MockRouter mr5 = new MockRouter(9994,"9995-3 9991-12 10000-4".split(" "));
        MockRouter mr6 = new MockRouter(9995,"9996-7 9994-3".split(" "));
        MockRouter mr7 = new MockRouter(9996,"9990-1 9993-4 9995-7".split(" "));
        MockRouter mr8 = new MockRouter(9997,"9992-3 9998-2".split(" "));
        MockRouter mr9 = new MockRouter(9998,"9997-2 9999-2".split(" "));
        MockRouter mr10 = new MockRouter(9999,"9998-2 10000-4".split(" "));
        MockRouter mr11 = new MockRouter(10000,"9999-4 9992-1".split(" "));

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
