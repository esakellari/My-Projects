package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * The class that implements the fragmentation of the files.
 */
public class Fragmentation {

    /**
     *
     * @param data
     * @param dirPath
     * @return
     * @throws IOException
     */
    public static ArrayList<String> fragment(byte[] data, String dirPath)
        throws IOException {

        ArrayList<String> pieces = new ArrayList<>();
        Path path;

        int blockSize = 2048;
        int blockNum = 0;

        // Calculate the bytes that may be left for the last fragment.
        int bytesRemaining = data.length % blockSize;
        int numOfBlocks = data.length / blockSize;

        for (int i = 0; i < numOfBlocks; i++) {
            byte[] block = new byte[blockSize];
            for (int j = 0; j < blockSize; j++) {
                block[j] = data[blockNum];
                blockNum++;
            }
            String name = dirPath + "Temp_" + i;
            pieces.add("Temp_" + i);
            path = Paths.get(name);
            Files.write(path, block);
        }

        // Write final bytes to the last file.
        if (bytesRemaining > 0) {
            byte[] block = new byte[bytesRemaining];
            for (int i = 0; i < bytesRemaining; i++) {
                block[i] = data[blockNum];
                blockNum++;
            }
            path = Paths.get(dirPath + "Temp_" + numOfBlocks);
            pieces.add("Temp_" + numOfBlocks);
            Files.write(path, block);
        }
        return pieces;
    }

    /**
     *
     * @param pieces
     * @param file
     * @param fileSize
     * @throws IOException
     */
    public static void defragment(ArrayList<String> pieces, String file, int fileSize)
        throws IOException {

        int blockSize = 2048;
        int blockNum = 0;
        int totalSize = fileSize;
        byte[] data = new byte[totalSize];

        // Calculate the bytes that may be left for the last fragment.
        int bytesRemaining = totalSize % blockSize;
        int numOfBlocks = totalSize / blockSize;

        for (int i = 0; i < numOfBlocks; i++) {
            Path path = Paths.get(pieces.get(i));
            byte[] block = Files.readAllBytes(path);
            for (int j = 0; j < blockSize; j++) {
                data[blockNum] = block[j];
                blockNum++;
            }
        }

        if (bytesRemaining > 0) {
            Path path = Paths.get(pieces.get(numOfBlocks));
            byte[] block = Files.readAllBytes(path);
            for (int j = 0; j < bytesRemaining; j++) {
                data[blockNum] = block[j];
                blockNum++;
            }
        }

        // Write all the contents to the final file.
        Path path = Paths.get(file);
        Files.write(path, data);
    }

}
