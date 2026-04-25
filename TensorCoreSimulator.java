import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class TensorCoreSimulator {

    // Matrix size is going to be N = 1000
    private static final int N = 1000;

    //Global Matrices
    private static int[][] matrixA = new int[N][N];
    private static int[][] matrixB = new int[N][N];
    private static int[][] matrixC = new int[N][N];

    public static long totalOps = 0;

    private static final ReentrantLock opsLock = new ReentrantLock();

    // Semaphore with 2 permits
    private static final Semaphore coreSemaphore = new Semaphore(2);

    public static void main(String[] args) {
        System.out.println("=== Tensor Core Simulator (N=" + N + ") ===");
        System.out.println("Generating data...");
        initializeMatrices();

        // Part A:

        System.out.println("--- Part A: Sequential Execution");
        long startTime = System.currentTimeMillis();
        runSequential();
        long endTime = System.currentTimeMillis();
        System.out.println("Sequential Time: " + (endTime - startTime) + " ms");

        // Part B:
        System.out.println("Part B: Parallel Execution (4 Threads)");
        resetResultMatrix();
        startTime = System.currentTimeMillis();
        runParallel(4, false); // 4 threads as per sample output [cite: 40]
        endTime = System.currentTimeMillis();
        System.out.println("Parallel Time: " + (endTime - startTime) + " ms");

        // Part C & Part D:
        System.out.println("Part C & D: Parallel Execution with Audit (Billing)");
        resetResultMatrix();
        totalOps = 0;
        startTime = System.currentTimeMillis();

        runParallel(8, true);

        endTime = System.currentTimeMillis();
        System.out.println("Safe Parallel Time: " + (endTime - startTime) + " ms");

        System.out.println("Total Operations Logged: " + totalOps);
        System.out.println("Expected Operations:");
        System.out.println((long)N * N * N);
    }

    // matrix multiplication using one thread

    private static void runSequential() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < N; k++) {
                    matrixC[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }
    }

    private static void runParallel(int threadCount, boolean enableAudit) {
        Thread[] threads = new Thread[threadCount];
        int rowsPerThread = N / threadCount;

        for (int i = 0; i < threadCount; i++) {
            int startRow = i * rowsPerThread;
            int endRow = (i == threadCount - 1) ? N : startRow + rowsPerThread;

            MatrixWorker worker = new MatrixWorker(startRow, endRow, enableAudit);
            threads[i] = new Thread(worker);
            threads[i].start();
        }
        try {
            for (Thread t : threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //random integer matrices:
    private static void initializeMatrices() {
        Random rand = new Random();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                matrixA[i][j] = rand.nextInt(10);
                matrixB[i][j] = rand.nextInt(10);
            }
        }
    }

    private static void resetResultMatrix() {
        matrixC = new int[N][N];
    }

    static class MatrixWorker implements Runnable {
        private final int startRow;
        private final int endRow;
        private final boolean enableAudit;

        public MatrixWorker(int startRow, int endRow, boolean enableAudit) {
            this.startRow = startRow;
            this.endRow = endRow;
            this.enableAudit = enableAudit;
        }

        @Override
        public void run() {
            if (enableAudit) {
                try {
                    coreSemaphore.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            try {
                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < N; j++) {
                        for (int k = 0; k < N; k++) {
                            matrixC[i][j] += matrixA[i][k] * matrixB[k][j];

                            if (enableAudit) {
                                opsLock.lock();
                                try {
                                    totalOps++;
                                } finally {
                                    opsLock.unlock();
                                }
                            }
                        }
                    }
                }
            } finally {
                if (enableAudit) {
                    coreSemaphore.release();
                }
            }
        }
    }
}