import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ListThread implements Runnable {
    Thread thread;
    String threadName;
    int repetitions = 0;
    int numLists = 0;
    List[] lists;

    ListThread(String threadName, List[] lists, int numLists, int repetitions) {
        this.threadName = threadName;
        this.lists = lists;
        this.repetitions = repetitions;
        this.numLists = numLists;
    }

    public void start() {
        if (this.thread == null) {
            this.thread = new Thread(this, this.threadName);
            this.thread.start();
        }
    }

    public void join() throws InterruptedException {
        this.thread.join();
    }

    public void run() {
        Random rand = new Random();
        int bound = rand.nextInt(500);

        for(int i = 0; i < repetitions; i++) {
           int randomNumber = rand.nextInt(bound);
           // The thread will choose randomly among the lists, the one on which
           // it will operate.
           int randomList = rand.nextInt(numLists);
           //Choose the action you will take next.
           switch(randomNumber % 3) {
            case 0: this.lists[randomList].prepend("Node"+ i + "_" + this.threadName);
                    break;
            case 1: this.lists[randomList].pop();
                    break;
            case 2: this.lists[randomList].head();
                    break;
           }
        }
    }
}