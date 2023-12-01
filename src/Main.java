import java.util.concurrent.*;

class AsyncBlockingQueue<T> {
    private final BlockingQueue<T> blockingQueue;
    private final ExecutorService executor;

    public AsyncBlockingQueue(int capacity, ExecutorService executor) {
        this.blockingQueue = new ArrayBlockingQueue<>(capacity);
        this.executor = executor;
    }

    public AsyncBlockingQueue(int capacity) {
        this(capacity, Executors.newCachedThreadPool());
    }

    public CompletableFuture<Void> asyncInsert(T element) {
        return CompletableFuture.runAsync(() -> {
            try {
                blockingQueue.put(element);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error inserting element into the queue", e);
            }
        }, executor);
    }

    public CompletableFuture<T> asyncRemove() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return blockingQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error removing element from the queue", e);
            }
        }, executor);
    }

    public T blockingRemove() throws InterruptedException {
        return blockingQueue.take();
    }


    public void blockingInsert(T element) throws InterruptedException {
        blockingQueue.put(element);
    }

    public void shutdown() {
        executor.shutdown();
    }
}



public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        AsyncBlockingQueue<String> asyncQueue = new AsyncBlockingQueue<>(2);

        // Асинхронная вставка
        CompletableFuture<Void> asyncInsertFuture = asyncQueue.asyncInsert("One")
                .thenRun(() -> {
                    System.out.println("Async insert completed on thread: " + Thread.currentThread().getName());
                });

        // Асинхронное получение
        CompletableFuture<Void> asyncRemoveFuture = asyncQueue.asyncRemove()
                .thenAccept(value -> {
                    System.out.println("Async remove completed on thread: " + Thread.currentThread().getName() + ", Value: " + value);
                });

        // Блокирующая вставка
        asyncQueue.blockingInsert("Two");
        System.out.println("Blocking insert completed on thread: " + Thread.currentThread().getName());

        // Блокирующее получение
        String result = asyncQueue.blockingRemove();
        System.out.println("Blocking remove completed on thread: " + Thread.currentThread().getName() + ", Value: " + result);

        // Асинхронная вставка в цикле
        CompletableFuture<?>[] asyncInsertLoopFutures = new CompletableFuture[5];
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            asyncInsertLoopFutures[i] = asyncQueue.asyncInsert("AsyncElement" + finalI)
                    .thenRun(() -> {
                        System.out.println("Async insert completed on thread: " + Thread.currentThread().getName() + ", Iteration: " + finalI);
                    });
        }
        CompletableFuture<Void> allAsyncInserts = CompletableFuture.allOf(asyncInsertLoopFutures);

        // Асинхронное получение в цикле
        CompletableFuture<?>[] asyncRemoveLoopFutures = new CompletableFuture[5];
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            asyncRemoveLoopFutures[i] = asyncQueue.asyncRemove()
                    .thenAccept(value -> {
                        System.out.println("Async remove completed on thread: " + Thread.currentThread().getName() + ", Value: " + value + ", Iteration: " + finalI);
                    });
        }
        CompletableFuture<Void> allAsyncRemoves = CompletableFuture.allOf(asyncRemoveLoopFutures);

        CompletableFuture.allOf(asyncInsertFuture, asyncRemoveFuture, allAsyncInserts, allAsyncRemoves).join();

        asyncQueue.shutdown();
    }
}