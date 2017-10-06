import node.Node;
import node.NodeImpl;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

/**
 * 
 */
public class Benchmark {
    
    public static void main(String args[])
        throws RemoteException, NoSuchAlgorithmException,
        NoSuchProviderException, NotBoundException,
        AlreadyBoundException {

        HashMap<Integer, Node> nodes = new HashMap<>();

        ArrayList<Node> nodesList;
        int rand;
        Node randNode;
        Node node;
        int nodeIndex;
        
        printHelp();
        
        Scanner scan = new Scanner(System.in);
        String input = scan.nextLine();

        while (!input.equals("exit")) {
            try {
                String[] inputs = input.split(" ");
                switch (inputs[0]) {
                    case "join":

                        int n = Integer.parseInt(inputs[1]);
                        int size = nodes.size();
                        String name = null;
                        for (int i = size; i < size + n; i++) {
                        	
                        	if (!nodes.isEmpty()) {
                        		// Choose the node from which the routing will begin
                        		nodesList = new ArrayList<>(nodes.values());

                        		rand = new SecureRandom().nextInt(nodesList.size());
                        		randNode = nodesList.get(rand);
                        		name = randNode.getName();
                        	} 
                        	
                            Node newNode = new NodeImpl(
                                String.valueOf(i),
                                "localhost",
                                name,
                                "localhost",
                                inputs[2]
                            );

                            nodes.put(i, newNode);
                        }

                        break;

                    case "get":
                        nodeIndex = Integer.parseInt(inputs[1]);

                        if (nodes.containsKey(nodeIndex)) {
                            node = nodes.get(nodeIndex);
                            node.retrieveFile(
                                inputs[2],
                                inputs[3],
                                Integer.parseInt(inputs[4])
                            );
                        } else {
                            System.out.println("This node does not exist.");
                        }

                        break;

                    case "put":
                        nodeIndex = Integer.parseInt(inputs[1]);
                        
                        if (nodes.containsKey(nodeIndex)) {
                            randNode = nodes.get(nodeIndex);
                            randNode.insertFile(
                                inputs[2],
                                Integer.parseInt(inputs[3])
                            );
                        } else {
                            System.out.println("This node does not exist.");
                        }

                        break;

                    case "departNode":
                        node = nodes.get(Integer.parseInt(inputs[1]));

                        nodes.remove(Integer.parseInt(inputs[1]));

                        System.out.println("Node " + node.getName() +
                            " will depart from the network.");
                        node.getRegistry().unbind(inputs[1]);

                        break;

                    case "depart":
                        nodesList = new ArrayList<>(nodes.values());

                        int numNodes = Integer.parseInt(inputs[1]);

                        for (int i = 0; i < numNodes; i++) {
                            rand = (new SecureRandom()).nextInt(nodesList.size());
                            node = nodesList.get(rand);
                            nodesList.remove(rand);
                            nodes.remove(Integer.parseInt(node.getName()));

                            System.out.println("Node " + node.getName() +
                                " will depart from the network.");
                            node.getRegistry().unbind(node.getName());
                        }

                        break;

                    case "printEntries":

                        nodesList = new ArrayList<>(nodes.values());

                        Iterator<Node> it = nodesList.iterator();

                        while (it.hasNext()) {
                            node = it.next();
                            node.printEntries();
                        }

                        break;

                    case "printNode":

                        if (nodes.containsKey(Integer.parseInt(inputs[1]))) {
                            node = nodes.get(Integer.parseInt(inputs[1]));
                            System.out.println("Node " + node.getName()
                                + " with public key "
                                + node.getPublicKey());
                        } else {
                            System.out.println("This node does not exist.");
                        }

                        break;

                    case "printNodeEntries":

                        if (nodes.containsKey(Integer.parseInt(inputs[1]))) {
                            node = nodes.get(Integer.parseInt(inputs[1]));
                            node.printEntries();
                        } else {
                            System.out.println("This node does not exist.");
                        }

                        break;
                    case "printAll":
                        nodesList = new ArrayList<>(nodes.values());

                        it = nodesList.iterator();

                        while (it.hasNext()) {
                            node = it.next();
                            System.out.println("Node " + node.getName()
                                + " with public key "
                                + node.getPublicKey());
                        }

                        break;
                }
            } catch (Exception e) {
                System.out.println("Wrong command. "
                    + e.getMessage()
                    + e.getLocalizedMessage());
                e.printStackTrace();
            }

            printHelp();

            input = scan.nextLine();
        }
        scan.close();
        System.exit(0);
    }

    /**
     *
     */
    private static void printHelp() {
        System.out.println(
            "\nUsage:\n" +
                "join <number of nodes to join> <path of node folders>\n" +
                "put <node ID> <file name> <hops>\n" +
                "get <node from which we will search> <decryption stringID> <publicKey> <hops>\n" +
                "departNode <node name>\n" +
                "depart <number of random nodes to depart>\n" +
                "printAll\n" +
                "printNode <node name>\n" +
                "printEntries\n" +
                "printNodeEntries <node name>\n" +
                "exit\n"
        );
    }

}
