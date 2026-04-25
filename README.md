# Tensor Core Simulator

## What is this?
This is a Java-based simulator I built to model how hardware accelerators process large-scale matrix multiplications. It runs operations on massive 1000x1000 matrices and focuses heavily on multi-threading, concurrency control, and simulating physical hardware limitations. 

## Core Components
I structured the simulation around a central dispatcher and worker threads to keep the workload organized:

* **TensorCoreSimulator:** The main controller that generates the global matrices, dispatches the parallel execution phases, and tracks the system's performance metrics.
* **MatrixWorker:** A custom class implementing `Runnable` that defines the exact task logic for the parallel threads. By assigning specific row ranges (`startRow` to `endRow`) to each worker, it ensures threads process their chunks independently without overwriting each other's calculations in the final result matrix.

## Concurrency & Hardware Simulation
The most interesting challenge of this project was handling the thread safety and resource throttling:

* **Simulating Hardware Limits (Semaphores):** I needed to spawn 8 distinct threads but simulate a physical environment where only 2 processing cores exist. I implemented a `java.util.concurrent.Semaphore` with exactly 2 permits. This safely throttles the system, ensuring only two threads can enter the heavy execution phase simultaneously.
* **Thread-Safe Auditing (Locks):** The system tracks a "billing audit" that increments a counter up to 1,000,000,000 total operations. Initially, race conditions caused the final count to drop when multiple threads updated the counter simultaneously. I fixed this by wrapping the increment operation in a critical section using a `ReentrantLock`, guaranteeing that only one thread can update the audit at a time.

## Performance Gains
Moving from a standard sequential approach to a synchronized parallel architecture ended up yielding massive speed improvements. Here is a quick look at the performance scaling:

| Execution Type | Threads Active | Approximate Processing Time |
| :--- | :--- | :--- |
| Sequential Baseline | 1 | ~2400 ms |
| Parallel Execution | 4 | ~650 ms |
