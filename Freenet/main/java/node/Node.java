package node;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 *
 */
public interface Node extends Remote {

    String getName() throws RemoteException;

    String getKey() throws RemoteException;

    String getPublicKey() throws RemoteException;

    Registry getRegistry() throws RemoteException;

    void insert(String fullPath, String fileName, int hops)
        throws NoSuchAlgorithmException,
        NoSuchProviderException, InvalidKeyException,
        SignatureException, NoSuchPaddingException,
        InvalidAlgorithmParameterException,
        IllegalBlockSizeException, BadPaddingException,
        IOException, NotBoundException;

    void retrieve(String stringID, String publicKeyString, int hops)
        throws NoSuchAlgorithmException, InvalidKeySpecException,
        InvalidKeyException, SignatureException,
        NoSuchPaddingException, InvalidAlgorithmParameterException,
        IllegalBlockSizeException, BadPaddingException,
        IOException, NotBoundException;

    void retrieveFile(String stringID, String publicKeyString, int hops)
        throws RemoteException;

    void insertFile(String fileName, int hops)
        throws RemoteException;

    Response get(String key, int hops, Set<String> nodesVisited)
        throws NotBoundException,
        IOException;

    byte[] join(RemoteNodeInfo remoteNodeInfo, byte[] hash, int hops)
        throws RemoteException, NoSuchAlgorithmException, NotBoundException;

    void printEntries() throws RemoteException, NotBoundException;

    Response put(FileInfo fileInfo, String fileKey, int hops,
                        Set<String> nodesVisited)
        throws NoSuchAlgorithmException,
        IOException, NotBoundException;

    /**
     *
     */
    class FileInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private byte[] signature;
        private byte[] initializationVector;
        private byte[] data;
        private String fileName;
        private int fileSize;

        FileInfo(byte[] sig, byte[] initVector, byte[] d, String fname,
                 int fSize) {
            signature = sig;
            initializationVector = initVector;
            fileName = fname;
            fileSize = fSize;
            data = d;
        }

        byte[] getInitializationVector() {
            return initializationVector;
        }

        byte[] getSignature() {
            return signature;
        }

        String getFileName() {
            return fileName;
        }

        int getFileSize() {
            return fileSize;
        }

        byte[] getData() {
            return data;
        }
    }

    /**
     * The class keeps the info about the nodes in the routing table. The
     * information kept is the key, the hostname, the port and the name of the
     * remote node.
     */
    class RemoteNodeInfo implements Serializable {

        private static final long serialVersionUID = 1L;
        String hostname;
        int port;
        String name;
        String key;

        RemoteNodeInfo(String _name, String k, String host, int p) {
            hostname = host;
            port = p;
            name = _name;
            key = k;
        }

        String getHostname() {
            return hostname;
        }

        int getPort() {
            return port;
        }

        String getName() {
            return name;
        }

        String getKey() {
            return key;
        }
    }

    /**
     * The class for responses between the nodes. The nodes create a new object
     * of this class to forward to the upstream requestor, in order to inform
     * whether they found what they were looking for, or not. In case they have
     * found the data they were looking for, these are included in the response
     * in the fileInfo field.
     */
    class Response implements Serializable {

        private static final long serialVersionUID = 1L;
        RemoteNodeInfo remoteNodeInfo;
        int response;
        FileInfo fileInfo;
        int hops;

        Response(RemoteNodeInfo nInfo, int resp) {
            response = resp;
            remoteNodeInfo = nInfo;
        }

        Response(RemoteNodeInfo nInfo, int response, FileInfo fileInfo, int hops) {
            remoteNodeInfo = nInfo;
            this.response  = response;
            this.fileInfo  = fileInfo;
            this.hops      = hops;
        }

        FileInfo getData() {
            return fileInfo;
        }

        RemoteNodeInfo getRemoteNodeInfo() {
            return remoteNodeInfo;
        }
    }

}
