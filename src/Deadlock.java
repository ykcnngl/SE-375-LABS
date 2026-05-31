// LAB 4
public class Deadlock {

    public static void main(String[] args) {
        DeadlockChopstick[] c = {new DeadlockChopstick(), new DeadlockChopstick(), new DeadlockChopstick()};
        new DeadlockPhilosopher(1, c[0], c[1]).start();
        new DeadlockPhilosopher(2, c[1], c[2]).start();
        new DeadlockPhilosopher(3, c[2], c[0]).start();
    }
}
class DeadlockChopstick {}

class DeadlockPhilosopher extends Thread {
    int id;
    DeadlockChopstick left, right;

    DeadlockPhilosopher(int id, DeadlockChopstick left, DeadlockChopstick right) {
        this.id = id;
        this.left = left;
        this.right = right;
    }

    public void run() {
        try {
            while (true) {
                synchronized (left) {
                    System.out.println("Filozof " + id + " sol çubuğu aldı.");
                    Thread.sleep(100);
                    synchronized (right) {
                        System.out.println("Filozof " + id + " yemek yiyor.");
                    }
                }
            }
        } catch (Exception e) {}
    }
}
