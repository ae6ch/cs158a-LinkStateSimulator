# cs158a-LinkStateSimulator
For the coding part, I want you to write a program, LinkStateSimulator.java, for the link state protocol and compare it to the behavior of distance vector routing for router in a line (the situation in class where we had a count-to-infinity problem). Your program will be compiled from the command-line by the grader using the line:

javac LinkStateSimulator.java
Your program will be run with a command of the form:

java LinkStateSimulator some_topology_file
Here some_topology_file is the file name of a file containing the network topology to use for the test. For example, the grader might run program with the line:

java LinkStateSimulator test_topology1.txt
The topology file should consist of a sequence of newline terminated lines of the form:

router_port adjacent_router_1_port-distance_1 ... adjacent_router_n_port-distance_n
Items within a line are space separated. The adjacent_router_i portion of adjacent_router_i_port-distance_i should be a valid TCP/UDP port number. The distance_i portion of adjacent_router_i_port-distance should be an integer distance. For example, some_topology_file might look like

13370 11380-1
11380 13370-1 11390-5 11400-7
11390 11380-5
11400 11380-7
When run, your LinkStateSimulator should cycle through the lines of some_topology_file, and for each line, instantiate an appropriate instance of a class MockRouter. Its constructor should take an int portNumber, String[] adjacents so can be fed the port and adjacent routers from the given line in the file. MockRouter should instantiate two Thread's. The run() method of the first thread should create a ServerSocket bound to portNumber. When it accepts a connections, it expects to receive one newline terminated message to which it then responds before closing the connection. MockRouter should be able to respond to three possible messages: (a) link state messages to which it responds with ACK and a newline, (b) h followed by a newline (a history message) to which it responds with all the link state messages its received (and at what time since the start of the simulation), followed by a line with the word TABLE followed by lines giving its routing table, (c) s followed by a newline (stop) to which it responds with STOPPING and a newline and then it stops its thread.

A link state message should be a line with format:

l sender_port seq_number time_to_live adjacent_router_1_port-distance_1 ... adjacent_router_n_port-distance_n
The second of MockRouter's Thread's run methods should be every 3 + random float between 0 and 1 seconds cycle through each of its neighbors making Socket connections to the neighbor router's port and sending all link state messages it has (its own and those it needs to forward) to its adjacent neighbors. Sequence numbers and time to live should be integers and the time to live should be updated as per the algorithm in the book/class. This thread should also compute a new routing table using Dijkstra's algorithm based on messages received from other routers by the other SocketServer thread.

Your LinkStateSimulator should start() each of a given MockRouter's threads after instantiation. It should then say "Initialization complete. Ready to accept commands." It should then go into a mode where it receives commands from the user, issues responses, and loop. Allowed commands are:

h some_port_number #returns the result of sending an h message to the router some_port_number
s some_port_number #shuts down MockRouter some_port_number
e #exits the program
Given your simulator is working you can construct networks with varying numbers of nodes in a line. Using the history command (h) conduct experiments to see how quick the routes converge for different nodes in your network of a given total number of nodes. Conduct further experiments to see the effect of shutting down a node on how long it takes for the network to re-stabilize.

In addition to these experiments, to gain more practice using netstat and nmap, run each of tools during at least one of your simulations and create a file transcripts.txt with the contents of running these commands. For each of these transcript have a sentence or two explaining what sockets you saw open related to your NetworkSimulator and what ports you saw in use.

