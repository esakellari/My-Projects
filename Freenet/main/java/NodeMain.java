import node.Node;
import node.NodeImpl;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/**
 * 
 */
public class NodeMain {

    /**
     * 
     * @param args
     * @throws NoSuchAlgorithmException
     * @throws RemoteException
     * @throws AlreadyBoundException
     * @throws NotBoundException
     */
    public static void main(String[] args)
        throws NoSuchAlgorithmException,
        RemoteException, AlreadyBoundException,
        NotBoundException {

        String name = args[0];
        String remoteNodeName = (args.length < 3) ? null : args[2];

        // args[1] = folder path
        Node node = new NodeImpl(name, "localhost", remoteNodeName,
            "localhost", args[1]);

        printHelp(name);

        Scanner scan = new Scanner(System.in);
        String input = scan.nextLine();

        while (!input.equals("exit")) {
            try {
                String[] inputs = input.split(" ");
                switch (inputs[0]) {
                    case "put":
                        //TODO : require full path
                        node.insertFile(inputs[1],
                            Integer.parseInt(inputs[2]));
                        break;

                    case "get":
                        //inputs[1] string id
                        //inputs[2] public key of owner
                        node.retrieveFile(inputs[1], inputs[2],
                            Integer.parseInt(inputs[3]));
                        break;

                    case "printEntries":
                        node.printEntries();
                        break;
                    case "printPublicKey":
                        System.out.println("Node " + name + " has public key: " + node.getPublicKey());
                        break;
                        
                    case "depart":
                        System.out.println("Node " + node.getName() +
                            " will depart from the network.");
                        node.getRegistry().unbind(name);

                        System.exit(0);
                        break;
                }
            } catch (Exception e) {
                System.out.println("Wrong command. " + e.getMessage()
                    + e.getLocalizedMessage());
                e.printStackTrace();
            }

            printHelp(name);

            input = scan.nextLine();
        }
        scan.close();
    }

    /**
     * 
     * @param nodeName
     */
    private static void printHelp(String nodeName) {

        System.out.println(
            "\nNode " + nodeName +
            "Usage:\n" +
            "put <file name> <max hops>\n" +
            "get <decryption stringID> <publicKey> <max hops>\n" +
            "depart\n" +
            "printEntries\n" +
            "printPublicKey\n\n"
        );
    }
    
}
