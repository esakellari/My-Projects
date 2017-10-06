package node;

import util.*;

import java.rmi.server.UnicastRemoteObject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 *
 */
public class NodeImpl extends UnicastRemoteObject implements Node {

    private static final long serialVersionUID = 1L;
    private ConcurrentHashMap<String, RemoteNodeInfo> routingTable;
    private String myKey;
    private String name;
    private ConcurrentHashMap<String, FileInfo> dataTable;
    private String dirPath;
    private String hostname;
    private int port;

    private int filesNum;
    private Registry registry;

    PrivateKey privateKey;
    PublicKey publicKey;

    static final int OK = 0;
    static final int EXISTS = 1;
    static final int NOT_FOUND = 2;
    static final int LOOP = 3;
    static final int CONTINUE = 4;

    /**
     * Node object constructor
     *
     * @param _name
     * @param myHost
     * @param remoteName
     * @param remoteHost
     * @param hops
     * @throws NoSuchAlgorithmException
     * @throws RemoteException
     * @throws AlreadyBoundException
     * @throws NotBoundException
     */
    public NodeImpl(String _name, String myHost,
                    String remoteName, String remoteHost, String path)
        throws NoSuchAlgorithmException, RemoteException,
        AlreadyBoundException, NotBoundException {

        name = _name;
        hostname = myHost;
        filesNum = 0;
        int hops = 5;

        routingTable = new ConcurrentHashMap<>();
        dataTable = new ConcurrentHashMap<>();

        // Create my public and private key pair.
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512, SecureRandom.getInstance("SHA1PRNG"));
        KeyPair pair = keyGen.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKey = pair.getPublic();

        // System.out.println("Node:"+ name
        //	+ ". My public key : "
        //	+ Base64.getEncoder().encodeToString(publicKey.getEncoded()));


        // Create my local folder where I will store files.
        new File(path + "/node_" + name + "/").mkdirs();
        dirPath = path + "/node_" + name + "/";

        // Generate random seed
        byte[] bytes = new byte[32];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(bytes);
        String seed = Base64.getEncoder().encodeToString(bytes);

        // Hash the seed
        byte[] hash = hash(seed);

        if (remoteName != null) {
            // Find the remote object to start joining the network
            // If we were really on a remote network we would do
            // createRegistry all the time. But because this is a
            // simulation and everything is on the same host,
            // we use the same registry for all.
            registry = LocateRegistry.getRegistry(myHost, 1099);
            registry.rebind(name, this);

            Node remoteNode = (Node) registry.lookup(remoteName);

            RemoteNodeInfo nodeInfo = new RemoteNodeInfo(name, null, myHost, 1099);

            if (remoteNode == null) {
                System.out.println("Could not contact remote node " + remoteName
                    + " please choose another node. ");
                return;
            }
            RemoteNodeInfo remoteRemoteNodeInfo = new RemoteNodeInfo(remoteName,
                remoteNode.getKey(),
                remoteHost, 1099);
            // Add the only entry you know in the routing table
            routingTable.put(remoteNode.getKey(), remoteRemoteNodeInfo);

            // Send the hash of the seed to the first node
            // (Announcement)
            byte[] tempKey = remoteNode.join(nodeInfo, hash, hops);
            myKey = Base64.getEncoder().encodeToString(tempKey);

        } else {
            myKey = Base64.getEncoder().encodeToString(hash);
            // Put this object in the registry for remote access.
            //registry = LocateRegistry.createRegistry(1099);
            registry = LocateRegistry.getRegistry(myHost, 1099);
            registry.rebind(name, this);
        }
    }

    /**
     * Retrieve file method.
     *
     * @param stringID
     * @param publicKeyString
     * @param hops
     */
    public void retrieveFile(String stringID, String publicKeyString, int hops) {
        // Assign the job to a thread.
        (new Thread(new RetrieveThread(stringID, publicKeyString, hops))).start();
        // And now I can continue with other jobs...
    }

    /**
     * Retrieve method, assigned to a separate thread.
     *
     * @param stringID      Decryption string id
     * @param pubKeyString  Public key
     * @param hops          Maximum number of hops
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NotBoundException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws SignatureException
     */
    public void retrieve(String stringID, String pubKeyString, int hops)
        throws NoSuchAlgorithmException, InvalidKeySpecException,
        NotBoundException, FileNotFoundException, IOException,
        InvalidKeyException, NoSuchPaddingException,
        InvalidAlgorithmParameterException, IllegalBlockSizeException,
        BadPaddingException, SignatureException {

        // Derive file key from string ID and public key string.
        byte[] stringHash = hash(stringID);
        byte[] pkHash     = hash(pubKeyString);
        byte[] xored      = xor(stringHash, pkHash);
        byte[] xoredHash  = hash(Base64.getEncoder().encodeToString(xored));
        String fileKey    = Base64.getEncoder().encodeToString(xoredHash);

        // Derive public key from public key string.
        byte[] stringIDBytes          = Base64.getDecoder().decode(stringID);
        byte[] pubKeyBytes            = Base64.getDecoder().decode(pubKeyString);
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
        KeyFactory keyFactory         = KeyFactory.getInstance("RSA");
        PublicKey pubKey              = keyFactory.generatePublic(pubKeySpec);

        // Count time for statistics.
        long tStart = System.currentTimeMillis();
        
        // Retrieve indirect file.
        Set<String> nodesVisited = new HashSet<>();
        Response res             = get(fileKey, hops, nodesVisited);
        FileInfo fileInfo        = res.getData();

        if (res.response != OK) {
            System.out.println("Could not find indirect file with string ID: "
                + stringID);
            return;
        }

        // Create temporary dir for file parts.
        String tempDir = dirPath + "file" + filesNum + "/";
        File dir = new File(tempDir);
        dir.mkdir();
        filesNum++;

        // Store the file.
        byte[] data = fileInfo.getData();
        Path path = Paths.get(dirPath + fileInfo.getFileName());
        if (!Files.exists(path)) {
            Files.write(path, data);
        }

        // Decrypt the file.
        byte[] stringIdPart  = Arrays.copyOfRange(stringIDBytes, 0, 16);
        byte[] decryptedData = Encryption.decrypt(stringIdPart,
                                fileInfo.getInitializationVector(), data);

        String tempFileName = fileInfo.getFileName().replaceFirst(".encrypted", "");

        path = Paths.get(tempDir + tempFileName);
        Files.write(path, decryptedData);

        // Then verify the data.
        boolean verify = verifyData(decryptedData, pubKey, fileInfo.getSignature());

        if (!verify) {
            System.out.println("Could not verify signature for file "
                + fileInfo.getFileName());
            return;
        }

        // Retrieve the total size of the file which is kept with the
        // indirect file.
        int fileSize = fileInfo.getFileSize();

        // Read all the lines from the file to get the content hash keys
        // and the decryption keys.
        java.util.List<String> lines = Files.readAllLines(path);
        java.util.Iterator<String> it = lines.iterator();

        // The first line of the file is the original filename.
        String fileName = it.next();
        ArrayList<String> pieces = new ArrayList<>();
        // Array used for pathlength statistics.
        ArrayList<Integer> hopsArray = new ArrayList<>();

        while (it.hasNext()) {
            String chk = it.next();
            String decryptionKey = it.next();

            nodesVisited = new HashSet<>();

            // Search for the piece of file with this content hash key.
            res = get(chk, hops, nodesVisited);
            
            hopsArray.add(hops - res.hops);

            switch (res.response) {
                case OK:
                    //System.out.println("File was stored successfully.");
                    break;
                case EXISTS:
                    System.out.println("File already exists.");
                    break;
                case CONTINUE:
                    System.out.println("Could not contact as many "
                        + "nodes as needed.");
                    break;
                case LOOP:
                    System.out.println("Fell in a loop.");
                    break;
                case NOT_FOUND:
                    System.out.println("Cannot find piece of file with "
                        + "Content Hash Key: " + chk);
                    return;
            }

            fileInfo = res.getData();

            // Copy the file if does not exist.
            // Store the file too.
            path = Paths.get(dirPath + fileInfo.getFileName());
            if (!Files.exists(path)) {
                Files.write(path, data);
            }

            // First we need to decrypt the data.
            byte[] encryptedData = fileInfo.getData();
            byte[] decryptionKeyBytes = Base64.getDecoder().decode(decryptionKey);
            byte[] initVector = fileInfo.getInitializationVector();
            decryptedData = Encryption.decrypt(decryptionKeyBytes,
                initVector, encryptedData);

            // Verify the data.
            verify = verifyData(decryptedData, pubKey, fileInfo.getSignature());

            if (!verify) {
                // If you cannot verify one of the pieces stop the procedure.
                System.out.println("Could not verify signature of file "
                    + fileInfo.getFileName());
                return;
            }

            // Put the data on temp files.
            tempFileName = fileInfo.getFileName().replaceFirst(".encrypted", "");

            path = Paths.get(tempDir + tempFileName);
            Files.write(path, decryptedData);

            // Put all the fragments in the pieces list.
            pieces.add(tempDir + tempFileName);
        }

        // Calculate the average path length.
        Iterator<Integer> itH = hopsArray.iterator();

		int totalHops = 0;
		while (itH.hasNext()) {
			totalHops += itH.next();
		}

		float avgHops = (float) (totalHops / hopsArray.size());
		System.out.println("Average number of hops: " + avgHops);

		// Calculate the time for the retrieval of the file.
		long tEnd = System.currentTimeMillis();
		long tDelta = tEnd - tStart;
		double elapsedSeconds = tDelta / 1000.0;
		System.out.println("Time in milliseconds: " + tDelta + ". Time in seconds: " + elapsedSeconds);
		
        // When we have assembled all the fragments,
        // we combine them to get the final file.
        String finalFileName = dirPath + fileName;
        Fragmentation.defragment(pieces, finalFileName, fileSize);

        System.out.println("File with string ID :" + stringID
            + " was retrieved successfully.");

        // Delete the temporary directory.
        deleteDir(dir);
    }

    /**
     * Recursive get method This method is invoked on the remote nodes and
     * returns the response whether the data asked is found or not.
     *
     * @param key
     * @param hops
     * @param nodesVisited
     * @return
     * @throws NotBoundException
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Override
    public Response get(String key, int hops, Set<String> nodesVisited)
        throws NotBoundException, FileNotFoundException, IOException {

        int distance;
        int minDistance = -1;
        Node toForwardNode = null;
        Response response = null;

        // If this node has already been tried, then we have a loop.
        if (nodesVisited.contains(myKey)) {
            response = new Response(null, NOT_FOUND);
            return response;
        }

        // Else, add this node to the nodesVisited set.
        nodesVisited.add(myKey);

        // Print for debug and/or info about the nodes along the route.
        //System.out.println("Visiting node " + name);

        hops--;

        if (dataTable.containsKey(key)) {
            // If I have the data asked, return it.
            //System.out.println("Key found on node " + this.name);
            RemoteNodeInfo remoteNodeInfo = new RemoteNodeInfo(name, myKey, hostname, port);
            response = new Response(remoteNodeInfo, OK, dataTable.get(key), hops);
            return response;
        }

        // This is the last node in the chain.
        if (hops == 0) {
            // Not found
            response = new Response(null, NOT_FOUND);
            return response;
        }

        if (!routingTable.isEmpty()) {
            Set<String> keySet = new HashSet<>();
            keySet.addAll(routingTable.keySet());

            while (!keySet.isEmpty()) {
                String forwardKey = null;

                java.util.Iterator<String> it = keySet.iterator();

                while (it.hasNext()) {
                    String nextKey = it.next();
                    distance = Math.abs(key.compareTo(nextKey));

                    if ((distance < minDistance) || (minDistance < 0)) {
                        minDistance = distance;
                        forwardKey = nextKey;
                    }
                }
                keySet.remove(forwardKey);
                minDistance = -1;
                RemoteNodeInfo remoteNodeInfo = routingTable.get(forwardKey);
                toForwardNode = getRemoteNode(remoteNodeInfo.getName(),
                    remoteNodeInfo.getHostname(), remoteNodeInfo.getPort());
                // Check if you could contact this node.
                if (toForwardNode != null) {

                    response = toForwardNode.get(key, hops, nodesVisited);

                    if (response.response == OK) {
                        // The data has been found on another node.
                        // I will cache it too.
                        dataTable.put(key, response.getData());

                        FileInfo fileInfo = response.getData();

                        if (!routingTable.containsKey(response.getRemoteNodeInfo().
                            getKey())) {
                            // If I don't have an entry for
                            // the source in my routing table, I will add it.
                            routingTable.put(response.getRemoteNodeInfo().getKey(),
                                response.getRemoteNodeInfo());
                        }

                        // Store the file too.
                        Path path = Paths.get(dirPath + fileInfo.getFileName());
                        if (!Files.exists(path)) {
                            byte[] data = fileInfo.getData();
                            Files.write(path, data);
                        } else {
                            System.out.print("A file with this name "
                                + "already exists.");
                            return response;
                        }

                        // And I will forward the data to the upstream requestor.
                        return response;
                    }
                }
            }
        } else {
            response = new Response(null, NOT_FOUND);
            return response;
        }
        response = new Response(null, NOT_FOUND);
        return response;
    }

    /**
     * Insert file method.
     *
     * @param fullPath
     * @param fileName
     * @param hops
     */
    public void insertFile(String fileName, int hops) {
    	String fullPath = dirPath + fileName;
        // Assign the job to a thread.
        new Thread(new InsertThread(fullPath, fileName, hops)).start();
        // And now I can continue with other jobs...
    }

    /**
     * Insert method assigned to a separate thread.
     * @param fullPath
     * @param fileName
     * @param hops
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws SignatureException
     * @throws NotBoundException
     */
    @Override
    public void insert(String fullPath, String fileName, int hops)
        throws NoSuchAlgorithmException, IOException,
        InvalidKeyException, NoSuchPaddingException,
        InvalidAlgorithmParameterException,
        IllegalBlockSizeException, BadPaddingException,
        SignatureException, NotBoundException {

        // Generate a random file identifier string.
        byte[] stringID = new byte[32];

        SecureRandom.getInstance("SHA1PRNG").nextBytes(stringID);

        byte[] stringHash = hash(Base64.getEncoder().encodeToString(stringID));
        System.out.println("Decryption StringID for " + fullPath + ": "
            + Base64.getEncoder().encodeToString(stringID));
        byte[] pkHash = hash(Base64.getEncoder().
            encodeToString(publicKey.getEncoded()));

        byte[] xored = xor(stringHash, pkHash);

        // File key of the original file inserted.
        String fileKey = Base64.getEncoder().encodeToString(hash(
            Base64.getEncoder().encodeToString(xored)));

        // Create a temp dir for the pieces of the file.
        String fileDir = dirPath + "file" + filesNum + "/";
        File dir = new File(fileDir);
        dir.mkdir();
        filesNum++;

        // Fragment the file.
        Path path = Paths.get(fullPath);
        byte[] data = Files.readAllBytes(path);
        int fileSize = data.length;

        ArrayList<String> pieces = Fragmentation.fragment(data, fileDir);
        ArrayList<String> contentHashKeys = new ArrayList<>();
        ArrayList<File> encryptedPieces = new ArrayList<>();

        // Create the indirect file that will hold the content hash keys.
        String indirectFileName = "indirect_" + stringID + "_.txt";

        File indirect = new File(fileDir + indirectFileName);
        FileOutputStream outStream = new FileOutputStream(indirect);

        BufferedWriter bufferedWriter
            = new BufferedWriter(new OutputStreamWriter(outStream));

        // First write the name of the original file.
        bufferedWriter.write(fileName);
        bufferedWriter.newLine();

        for(String fname : pieces) {
            path = Paths.get(fileDir + fname);
            byte[] content = Files.readAllBytes(path);

            // Retrieve the hash of the data as file key.
            String chk = Base64.getEncoder().encodeToString(contentHash(content));
            contentHashKeys.add(chk);

            // Write the content hash key in the indirect file.
            bufferedWriter.write(chk);
            bufferedWriter.newLine();

            // And encrypt the files.
            byte[] randFname = new byte[64];

            // Encrypt the data.
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            Path encryptedPath = Paths.get(fileDir + randFname + ".encrypted");

            random.nextBytes(randFname);

            // Produce an initialization vector.
            byte[] initializationVector = new byte[16];
            random.nextBytes(initializationVector);

            // Produce random encryption key.
            byte[] randKey = new byte[16];
            random.nextBytes(randKey);

            // Encrypt the data of the file.
            byte[] encryptedData = Encryption.encrypt(randKey,
                initializationVector,
                content);
            // Store it in a new file.
            Files.write(encryptedPath, encryptedData);

            encryptedPieces.add(encryptedPath.toFile());

            // Write the encryption key together with the content hash key.
            bufferedWriter.write(Base64.getEncoder().encodeToString(randKey));
            bufferedWriter.newLine();

            // Sign the file.
            Signature dsa = Signature.getInstance("SHA1withRSA");
            dsa.initSign(privateKey);

            dsa.update(content);

            byte[] signature = dsa.sign();

            FileInfo fileInfo = new FileInfo(
                signature,
                initializationVector,
                Files.readAllBytes(encryptedPath),
                encryptedPath.toFile().getName(),
                0
            );

            // Insert the file in the network.
            Set<String> nodesVisited = new HashSet<>();
            Response res = put(fileInfo, chk, hops, nodesVisited);

            switch (res.response) {
                case OK:
                    // System.out.println("File was stored successfully.");
                    break;
                case EXISTS:
                    System.out.println("File already exists.");
                    deleteDir(dir);
                    return;
                case CONTINUE:
                    System.out.println("Could not contact as many "
                        + "nodes as needed.");
                    deleteDir(dir);
                    return;
                case LOOP:
                    System.out.println("Fell in a loop.");
                    deleteDir(dir);
                    return;
            }
        }

        bufferedWriter.close();

        // Sign the indirect file.
        Signature dsa = Signature.getInstance("SHA1withRSA");
        dsa.initSign(privateKey);

        path = Paths.get(fileDir + indirectFileName);
        byte[] indirData = Files.readAllBytes(path);
        dsa.update(indirData);

        byte[] signature = dsa.sign();

        // Encrypt the data.
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        byte[] initializationVector = new byte[16];
        random.nextBytes(initializationVector);

        // Encrypt the indirect file with the unique and random stringID
        // produced.
        byte[] encryptedData = Encryption.encrypt(
            Arrays.copyOfRange(stringID, 0, 16),
            initializationVector,
            indirData
        );

        String encryptedIndirFileName = indirectFileName + ".encrypted";
        path = Paths.get(fileDir + encryptedIndirFileName);

        Files.write(path, encryptedData);

        // The indirect file will be stored under the file key.
        System.out.println("Indirect file key:" + fileKey);
        FileInfo fileInfo = new FileInfo(
            signature,
            initializationVector,
            Files.readAllBytes(path),
            encryptedIndirFileName,
            fileSize
        );

        // Insert the file in the network.
        Set<String> nodesVisited = new HashSet<>();
        Response res = put(fileInfo, fileKey, hops, nodesVisited);

        switch (res.response) {
            case OK:
                System.out.println("File with stringID "
                    + Base64.getEncoder().encodeToString(stringID)
                    + " was stored successfully.");
                break;
            case EXISTS:
                System.out.println("File already exists.");
                deleteDir(dir);
                return;
            case CONTINUE:
                System.out.println("Could not contact as many "
                    + "nodes as needed.");
                deleteDir(dir);
                return;
            case LOOP:
                System.out.println("Fell in a loop.");
                deleteDir(dir);
                return;
        }

        // Now I can delete the temp dir.
        deleteDir(dir);
    }

    /**
     * Recursive put method. This method is invoked on the remote nodes and
     * returns the response whether the data is successfully stored or not.
     *
     * @param fileInfo
     * @param fileKey
     * @param hops
     * @param nodesVisited
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws NotBoundException
     */
    @Override
    public Response put(FileInfo fileInfo, String fileKey,
                        int hops, Set<String> nodesVisited)
        throws FileNotFoundException,
        IOException,
        NoSuchAlgorithmException, NotBoundException {

        int distance;
        int minDistance = -1;
        Node toForwardNode = null;
        Response response = null;

        // If this node has already been tried,
        // then we have a loop.
        if (nodesVisited.contains(myKey)) {
            response = new Response(null, LOOP);
            return response;
        }

        // Else, add this node to the nodesVisited set.
        nodesVisited.add(myKey);

        hops--;

        if (dataTable.containsKey(fileKey)) {
            System.out.println("This file already exists. Found on node "
                + this.name);
            RemoteNodeInfo remoteNodeInfo = new RemoteNodeInfo(name, myKey, hostname, port);
            response = new Response(remoteNodeInfo, EXISTS);
            return response;
        }

        // This is the last node in the chain.
        if (hops == 0) {
            // All clear
            dataTable.put(fileKey, fileInfo);

            RemoteNodeInfo remoteNodeInfo = new RemoteNodeInfo(name, myKey, hostname, port);

            response = new Response(remoteNodeInfo, OK);

            // Store the file too.
            Path path = Paths.get(dirPath + fileInfo.getFileName());
            if (!Files.exists(path)) {
                byte[] data = fileInfo.getData();
                Files.write(path, data);
            } else {
                System.out.print("A file with this name already exists.");
                return response;
            }

            return response;
        }

        if (!routingTable.isEmpty()) {

            Set<String> keySet = new HashSet<>();
            keySet.addAll(routingTable.keySet());
            String forwardKey = null;

            while (!keySet.isEmpty()) {
                for(String nextKey : keySet) {
                    distance = Math.abs(fileKey.compareTo(nextKey));
                    if ((distance < minDistance) || (minDistance < 0)) {
                        minDistance = distance;
                        forwardKey = nextKey;
                    }
                }
                keySet.remove(forwardKey);
                minDistance = -1;

                RemoteNodeInfo remoteNodeInfo = routingTable.get(forwardKey);
                toForwardNode = getRemoteNode(remoteNodeInfo.getName(),
                    remoteNodeInfo.getHostname(),
                    remoteNodeInfo.getPort());
                // Check if you could contact this remote node.
                if (toForwardNode != null) {

                    response = toForwardNode.put(fileInfo, fileKey, hops, nodesVisited);

                    if (response.response == OK || response.response == EXISTS) {

                        remoteNodeInfo = new RemoteNodeInfo(response.getRemoteNodeInfo().getName(),
                            response.getRemoteNodeInfo().getKey(),
                            response.getRemoteNodeInfo().getHostname(),
                            response.getRemoteNodeInfo().getPort());

                        response = new Response(remoteNodeInfo, response.response);

                        // Also add an entry for this node in my routing table
                        if (!routingTable.containsKey(response.getRemoteNodeInfo().
                            getKey())) {
                            routingTable.put(response.getRemoteNodeInfo().getKey(),
                                response.getRemoteNodeInfo());
                        }

                        dataTable.put(fileKey, fileInfo);

                        // Store the file too.
                        Path path = Paths.get(dirPath + fileInfo.getFileName());
                        if (!Files.exists(path)) {
                            byte[] data = fileInfo.getData();
                            Files.write(path, data);
                        } else {
                            System.out.print("A file with this name "
                                + "already exists.");
                            return response;
                        }

                        return response;
                    }
                }
            }
            if (hops > 0) {
                // If no collision has been found, but there are
                // nonzero hops remaining, we need to continue.
                response = new Response(null, CONTINUE);
                return response;

            }
            response = new Response(null, LOOP);
            return response;
        }
        // My routing table is empty, so all clear.
        if (hops > 0) {
            // If no collision has been found, but there are
            // nonzero hops remaining, we need to continue.
            RemoteNodeInfo remoteNodeInfo = new RemoteNodeInfo(name, myKey, hostname, port);
            response = new Response(remoteNodeInfo, CONTINUE);
        } else {
            RemoteNodeInfo remoteNodeInfo = new RemoteNodeInfo(name, myKey, hostname, port);
            response = new Response(remoteNodeInfo, OK);
        }

        dataTable.put(fileKey, fileInfo);
        // Store the file too.
        // Store the file too.
        Path path = Paths.get(dirPath + fileInfo.getFileName());
        if (!Files.exists(path)) {
            byte[] data = fileInfo.getData();
            Files.write(path, data);
        } else {
            System.out.println("A file with this name already exists.");
        }
        return response;
    }

    /**
     * The method for the new node 'announcement'. It recursively calls the
     * join methods of the remote nodes 'hops' times. The key of
     * the new node is calculated by during this procedure.
     *
     * @param remoteNodeInfo
     * @param hash
     * @param hops
     * @return
     * @throws NoSuchAlgorithmException
     * @throws RemoteException
     * @throws NotBoundException
     */
    public byte[] join(RemoteNodeInfo remoteNodeInfo, byte[] hash, int hops)
        throws NoSuchAlgorithmException,
        RemoteException, NotBoundException {

        hops--;

        // Generate seed
        byte[] seedBytes = new byte[32];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(seedBytes);

        // XOR seed with hash
        byte[] xored = xor(seedBytes, hash);

        // Hash again
        byte[] xoredHash = hash(Base64.getEncoder().encodeToString(xored));

        // If this is the last node in the chain
        if (hops == 0) {
            // Add the entry for the node into the routing table
            routingTable.put(Base64.getEncoder().encodeToString(xoredHash),
                remoteNodeInfo);
            return xoredHash;
        }

        byte[] key;
        if (!routingTable.isEmpty()) {
            // Choose randomly the node to forward
            // the announcement from the routing table
            Node remoteNode;
            ArrayList<RemoteNodeInfo> nodesList = new ArrayList<>(routingTable.values());
            do {
                int rand = SecureRandom.getInstance("SHA1PRNG").nextInt(nodesList.size());

                RemoteNodeInfo randRemoteNodeInfo = nodesList.get(rand);
                remoteNode = getRemoteNode(
                    randRemoteNodeInfo.getName(),
                    randRemoteNodeInfo.getHostname(),
                    randRemoteNodeInfo.getPort()
                );
                nodesList.remove(rand);
            } while ((remoteNode == null) && (!nodesList.isEmpty()));

            if (remoteNode != null) {
                key = remoteNode.join(remoteNodeInfo, xoredHash, hops);
            } else {
                key = xoredHash;
            }

        } else {
            key = xoredHash;
        }

        // Add the entry for the node in the routing table
        routingTable.put(Base64.getEncoder().encodeToString(key), remoteNodeInfo);

        return key;
    }

    /**
     *
     * @throws RemoteException
     * @throws NotBoundException
     */
    public void printEntries() throws RemoteException, NotBoundException {

        System.out.println("Entries of node " + name);

        ArrayList<RemoteNodeInfo> nodesList = new ArrayList<>(routingTable.values());

        for(RemoteNodeInfo entry : nodesList) {
            Node remoteNode = getRemoteNode(entry.getName(),
                entry.getHostname(),
                entry.getPort());
            if (remoteNode != null) {
                System.out.println("Entry:" + remoteNode.getName());
            }
        }
        System.out.println("----------------");
    }

    /**
     *
     * @return
     * @throws RemoteException
     */
    @Override
    public String getName() throws RemoteException {
        return name;
    }

    /**
     *
     * @return
     * @throws RemoteException
     */
    @Override
    public String getKey() throws RemoteException {
        return myKey;
    }

    /**
     *
     * @return
     * @throws RemoteException
     */
    @Override
    public String getPublicKey() throws RemoteException {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     *
     * @param str
     * @return
     * @throws NoSuchAlgorithmException
     */
    private byte[] hash(String str) throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));

        return hash;
    }

    /**
     * The Hash function to produce the Content Hash Key (CHK).
     *
     * @param contents
     * @return
     * @throws NoSuchAlgorithmException
     */
    private byte[] contentHash(byte[] contents) throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(contents);

        return hash;
    }

    /**
     * Verify signature method.
     *
     * @param data
     * @param pubKey
     * @param signature
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    private boolean verifyData(byte[] data, PublicKey pubKey, byte[] signature)
        throws NoSuchAlgorithmException,
        InvalidKeyException, SignatureException {

        // Verify the data.
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(pubKey);

        sig.update(data);

        boolean verifies = sig.verify(signature);

        return verifies;
    }

    /**
     * XOR method
     *
     * @param stringHash
     * @param pkHash
     * @return
     */
    private byte[] xor(byte[] stringHash, byte[] pkHash) {
        // XOR identifier with hash to produce the file key
        byte[] xored = new byte[32];
        for (int i = 0; i < xored.length; i++) {
            xored[i] = (byte) (stringHash[i] ^ pkHash[i]);
        }
        return xored;
    }

    /**
     * Get remote Node method.
     *
     * @param name
     * @param host
     * @param port
     * @return
     * @throws RemoteException
     * @throws NotBoundException
     */
    private Node getRemoteNode(String name, String host, int port)
        throws RemoteException, NotBoundException {

        Registry registry = LocateRegistry.getRegistry(host, port);

        String[] entries = registry.list();
        if (Arrays.asList(entries).contains(name)) {
            return (Node) registry.lookup(name);
        } else {
            return null;
        }
    }

    /**
     *
     * @param dir
     */
    private void deleteDir(File dir) {
        File[] files = dir.listFiles();

        // First delete all the files in the directory.
        for (File f : files) {
            f.delete();
        }

        // Finally delete the dir itself.
        dir.delete();
    }

    /**
     * Get registry method.
     *
     * @return
     * @throws RemoteException
     */
    public Registry getRegistry() throws RemoteException {
        return registry;
    }

    /**
     * Nested thread class for retrieving a file.
     */
    public class RetrieveThread implements Runnable {

        private String stringID;
        private String publicKeyString;
        private int hops;

        public RetrieveThread(String strID, String pkString, int h) {
            stringID = strID;
            publicKeyString = pkString;
            hops = h;
        }

        public void run() {
            try {
                retrieve(stringID, publicKeyString, hops);
            } catch (InvalidKeyException | NoSuchAlgorithmException
                | InvalidKeySpecException | NoSuchPaddingException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException
                | SignatureException | NotBoundException | IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Nested thread class for inserting a file.
     */
    public class InsertThread implements Runnable {

        private String fullPath;
        private String fileName;
        private int hops;

        public InsertThread(String fpath, String fname, int h) {
            fileName = fname;
            fullPath = fpath;
            hops = h;
        }

        public void run() {
            try {
                insert(fullPath, fileName, hops);
            } catch (InvalidKeyException | NoSuchAlgorithmException
                | SignatureException | NoSuchPaddingException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException
                | IOException | NotBoundException e1) {
                e1.printStackTrace();
            }
        }
    }
}
