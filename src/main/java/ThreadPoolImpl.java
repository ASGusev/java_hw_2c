import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

public class ThreadPoolImpl {
    private final int threadsNumber;
    private final Thread[] threads;
    private final Queue <Task> waitingTasks = new ArrayDeque<Task>();
    private boolean finish = false;

    ThreadPoolImpl(int threadsNumber) {
        this.threadsNumber = threadsNumber;

        threads = new Thread[threadsNumber];
        ThreadRoutine routine = new ThreadRoutine();
        for (int i = 0; i < threadsNumber; i++) {
            threads[i] = new Thread(routine);
            threads[i].start();
        }
    }

    public <S> LightFuture<S> submit(Supplier<S> job) {
        Task<S> task = new Task<S>(job);
        synchronized (waitingTasks) {
            waitingTasks.add(task);
            waitingTasks.notify();
        }
        return task;
    }

    public synchronized void shutdown() {
        if (finish) {
            return;
        }

        finish = true;
        for (Thread thread: threads) {
            thread.interrupt();
        }

        for (Task task: waitingTasks) {
            synchronized (task.resultExpectation) {
                task.resultExpectation.notifyAll();
            }
        }
    }

    private class ThreadRoutine implements Runnable{
        public void run() {
            while (!finish) {
                Task curTask = null;
                synchronized (waitingTasks) {
                    if (!waitingTasks.isEmpty()) {
                        curTask = waitingTasks.poll();
                    } else {
                        try {
                            waitingTasks.wait();
                        } catch (InterruptedException e) {}
                    }
                }
                if (curTask != null) {
                    curTask.execute();
                }
            }
        }
    }

    public class Task<T> implements LightFuture<T> {
        private volatile boolean ready = false;
        private Supplier<T> task;
        private T result = null;
        private final Boolean resultExpectation = false;
        private volatile boolean finishedWithException = false;
        private Queue<Task> successors = new ArrayDeque<>();

        Task(Supplier<T> task) {
            this.task = task;
        }

        protected void execute() {
            try {
                result = task.get();
            } catch (Throwable e) {
                finishedWithException = true;
            } finally {
                synchronized (resultExpectation) {
                    ready = true;
                    for (Task nextTask : successors) {
                        synchronized (waitingTasks) {
                            waitingTasks.add(nextTask);
                            waitingTasks.notify();
                        }
                    }
                    resultExpectation.notifyAll();
                }
            }
        };

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public T get() throws LightExecutionException, InterruptedException {
            synchronized (resultExpectation) {
                while (!ready && !finishedWithException && !finish) {
                    try {
                        resultExpectation.wait();
                    } catch (InterruptedException e) {}
                }
            }
            if (finishedWithException) {
                throw new LightExecutionException();
            }
            if (finish) {
                throw new InterruptedException();
            }
            return result;
        }

        @Override
        public <U> LightFuture<U> thenApply(Function<T,U> f) {
            Supplier<U> job = () -> {
                try {
                    return f.apply(Task.this.get());
                } catch (LightExecutionException | InterruptedException e) {
                    throw new PreviousTaskError();
                }
            };

            LightFuture<U> newTask;
            synchronized (resultExpectation) {
                if (ready) {
                    newTask = submit(job);
                } else {
                    newTask = new Task<U>(job);
                    successors.add((Task)newTask);
                }
            }
            return newTask;
        }
    }

    private static class PreviousTaskError extends Error {}
}
