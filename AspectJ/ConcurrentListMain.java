import java.io.IOException;
import java.util.Scanner;

public class ConcurrentListMain {

    public static void main(String[] args) throws InterruptedException {
        int numThreads, repetitions, numLists;
        List[] lists = null;
        Scanner scan = new Scanner(System.in);
        ListThread[] threads;
        int i,j;

        System.out.print("Please enter number of threads: ");
        numThreads = scan.nextInt();

        threads = new ListThread[numThreads];

        System.out.print("Please enter number of repetitions(i.e." +
                         " how many times a thread will do actions" +
                         " like 'pop', 'prepend', or 'head'): ");
        repetitions = scan.nextInt();

        System.out.print("Please enter how many lists you wish to have: ");
        numLists = scan.nextInt();

        // Create the lists. 
        lists = new List[numLists];
        for (i = 0; i < numLists; i++) {
            lists[i] = new List("List_" + i);
        }
 
        for (i = 0; i < numThreads ; i++) {
            threads[i] = new ListThread("Thread_" + i, lists,
                                        numLists, repetitions);
            threads[i].start();
        }

        // Wait for the threads to finish.
        for (i = 0; i < numThreads ; i++) {
            threads[i].join();
        }

        // Print the lists.
        for (i = 0; i < numLists; i++) {
            System.out.println("The final list is:");
            lists[i].printList();
        }
    }
}