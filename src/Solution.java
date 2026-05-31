// LAB 4

class Chopstick {}

class Philosopher extends Thread {
    int id;
    DeadlockChopstick first, second;

    Philosopher(int id, DeadlockChopstick left, DeadlockChopstick right) {
        this.id = id;
        if (id == 3) {
            this.first = right;
            this.second = left;
        } else {
            this.first = left;
            this.second = right;
        }
    }

    public void run() {
        try {
            while (true) {
                synchronized (first) {
                    System.out.println("Filozof " + id + " ilk çubuğu aldı.");
                    synchronized (second) {
                        System.out.println("Filozof " + id + " yemek yiyor.");
                        Thread.sleep(100);
                    }
                }
            }
        } catch (Exception e) {}
    }
}

public class Solution {
    public static void main(String[] args) {
        DeadlockChopstick[] c = {new DeadlockChopstick(), new DeadlockChopstick(), new DeadlockChopstick()};
        new DeadlockPhilosopher(1, c[0], c[1]).start();
        new DeadlockPhilosopher(2, c[1], c[2]).start();
        new DeadlockPhilosopher(3, c[2], c[0]).start();
    }
}