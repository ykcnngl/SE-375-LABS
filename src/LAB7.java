import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LAB7 {

    private static int bufferSize;
    private static int totalItemsToProduce;
    private static int producedCount = 0;
    private static int consumedCount = 0;

    private static long lockAcquireCount = 0;
    private static long lockReleaseCount = 0;
    private static long readLockAcquireCount = 0;
    private static long readLockReleaseCount = 0;
    private static long writeLockAcquireCount = 0;
    private static long writeLockReleaseCount = 0;

    private static final Queue<Integer> buffer = new LinkedList<>();

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Buffer size: ");
        bufferSize = scanner.nextInt();
        System.out.print("Number of producers: ");
        int numProducers = scanner.nextInt();
        System.out.print("Number of consumers: ");
        int numConsumers = scanner.nextInt();
        System.out.print("Total number of items to be produced: ");
        totalItemsToProduce = scanner.nextInt();

        System.out.println("\n--- Running Version 1: ReentrantLock ---");
        resetStats();
        long startTimeV1 = System.currentTimeMillis();
        runSimulation(numProducers, numConsumers, true);
        long endTimeV1 = System.currentTimeMillis();
        printResults("Reentrant Lock", endTimeV1 - startTimeV1);

        System.out.println("\n--- Running Version 2: ReentrantReadWriteLock ---");
        resetStats();
        long startTimeV2 = System.currentTimeMillis();
        runSimulation(numProducers, numConsumers, false);
        long endTimeV2 = System.currentTimeMillis();
        printResults("ReadWriteLock", endTimeV2 - startTimeV2);
    }

    private static void resetStats() {
        buffer.clear();
        producedCount = 0;
        consumedCount = 0;
        lockAcquireCount = 0;
        lockReleaseCount = 0;
        readLockAcquireCount = 0;
        readLockReleaseCount = 0;
        writeLockAcquireCount = 0;
        writeLockReleaseCount = 0;
    }

    private static void runSimulation(int numProducers, int numConsumers, boolean useV1) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();

        ReentrantLock v1Lock = new ReentrantLock();
        Condition notFull = v1Lock.newCondition();
        Condition notEmpty = v1Lock.newCondition();

        ReentrantReadWriteLock v2Lock = new ReentrantReadWriteLock();

        for (int i = 0; i < numProducers; i++) {
            threads.add(new Thread(() -> {
                while (true) {
                    if (useV1) {
                        v1Lock.lock(); lockAcquireCount++;
                        try {
                            if (producedCount >= totalItemsToProduce) break;
                            while (buffer.size() == bufferSize) notFull.await();
                            buffer.add(1);
                            producedCount++;
                            notEmpty.signalAll();
                        } catch (InterruptedException e) { break; }
                        finally { v1Lock.unlock(); lockReleaseCount++; }
                    } else {
                        v2Lock.writeLock().lock(); writeLockAcquireCount++;
                        try {
                            if (producedCount >= totalItemsToProduce) break;
                            if (buffer.size() < bufferSize) {
                                buffer.add(1);
                                producedCount++;
                            }
                        } finally { v2Lock.writeLock().unlock(); writeLockReleaseCount++; }
                    }
                }
            }));
        }

        for (int i = 0; i < numConsumers; i++) {
            threads.add(new Thread(() -> {
                while (true) {
                    if (useV1) {
                        v1Lock.lock(); lockAcquireCount++;
                        try {
                            if (consumedCount >= totalItemsToProduce) break;
                            while (buffer.isEmpty() && producedCount < totalItemsToProduce) notEmpty.await();
                            if (consumedCount >= totalItemsToProduce) break;
                            if (!buffer.isEmpty()) {
                                buffer.poll();
                                consumedCount++;
                                notFull.signalAll();
                            }
                        } catch (InterruptedException e) { break; }
                        finally { v1Lock.unlock(); lockReleaseCount++; }
                    } else {
                        v2Lock.readLock().lock(); readLockAcquireCount++;
                        boolean empty = buffer.isEmpty();
                        v2Lock.readLock().unlock(); readLockReleaseCount++;

                        v2Lock.writeLock().lock(); writeLockAcquireCount++;
                        try {
                            if (consumedCount >= totalItemsToProduce) break;
                            if (!buffer.isEmpty()) {
                                buffer.poll();
                                consumedCount++;
                            }
                        } finally { v2Lock.writeLock().unlock(); writeLockReleaseCount++; }
                    }
                }
            }));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
    }

    private static void printResults(String label, long duration) {
        System.out.println(label);
        System.out.println("Produced: " + producedCount);
        System.out.println("Consumed: " + consumedCount);
        if (label.equals("Reentrant Lock")) {
            System.out.println("Lock acquired: " + lockAcquireCount);
            System.out.println("Lock released: " + lockReleaseCount);
        } else {
            System.out.println("Read lock acquired: " + readLockAcquireCount);
            System.out.println("Read lock released: " + readLockReleaseCount);
            System.out.println("Write lock acquired: " + writeLockAcquireCount);
            System.out.println("Write lock released: " + writeLockReleaseCount);
        }
        System.out.println("Execution time: " + duration + " ms");
    }
}