package tokyo.peya.langjal.vm.engine.scheduler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskScheduler
{
    private final Map<Long, RegisteredTask> tasks;
    @Nullable
    private final ThreadPoolExecutor executor;

    private long taskIDCounter;

    public TaskScheduler(int threadPoolSize)
    {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize);
        this.tasks = new ConcurrentHashMap<>();
    }

    public TaskScheduler()
    {
        this.executor = null;  // シングルスレッドモード
        this.tasks = new ConcurrentHashMap<>();
    }


    public void submitTask(@NotNull VMRunnable runnable, long delay, long loop, boolean isAsync)
    {
        if (delay < 0 || loop < 0)
            throw new IllegalArgumentException("Delay and loop must be non-negative.");
        else if (runnable.getTaskID() != -1)
            throw new IllegalStateException("This runnable is already scheduled with task ID: " + runnable.getTaskID());

        long incrementalID = this.taskIDCounter++;
        RegisteredTask task = new RegisteredTask(incrementalID, runnable, delay, loop, isAsync);
        runnable.setTaskID(incrementalID);

        this.tasks.put(incrementalID, task);
    }

    public void cancelTask(long taskID)
    {
        RegisteredTask task = this.tasks.remove(taskID);
        if (task == null)
            return;

        if (!(task.future == null || task.future.isDone() || task.future.isCancelled()))
            task.future.cancel(true); // タスクがまだ実行中の場合はキャンセル
    }

    public void heartbeat()
    {
        for (RegisteredTask task : this.tasks.values())
        {
            task.exceedHeartbeats += 1;

            if (!task.hasRun && task.exceedHeartbeats < task.delayHeartbeats)
                continue;
            else if (task.loopHeartBeats > 0 && task.exceedHeartbeats < task.loopHeartBeats)
                continue;  // periodTicks が設定されている場合に，次の実行までの時間を待つ

            if (task.async)  // 非同期なら，スレッドプールで実行
            {
                if (this.executor == null)
                    throw new IllegalStateException("Cannot run async task in single-threaded mode.");
                task.future = this.executor.submit(() -> runTask(task));
            }
            else
            {
                // 同期なら，現在のスレッドで実行
                FutureTask<?> futureTask = new FutureTask<>(() -> runTask(task), null);
                task.future = futureTask;
                futureTask.run();
            }

            task.hasRun = true;

            if (task.loopHeartBeats > 0)
                task.exceedHeartbeats = 0;
            else
                this.tasks.remove(task.id);  // １度実行したのでタスクを削除
        }
    }

    private static void runTask(RegisteredTask task)
    {
        try
        {
            task.runnable.run();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("An exception occurred while executing task #" + task.id);
        }

        task.future = null;
    }

    public void shutdown()
    {
        try
        {
            if (this.executor != null)
                this.executor.shutdown();

            for (RegisteredTask task : this.tasks.values())
                if (!(task.future == null || task.future.isCancelled() || task.future.isDone()))
                    task.future.cancel(true);

            // とりあえず 5秒待ってみる
            if (!(this.executor == null || this.executor.awaitTermination(5, TimeUnit.SECONDS)))
                this.executor.shutdownNow();
        }
        catch (InterruptedException _)
        {
        }
    }

    @Getter
    private static class RegisteredTask
    {
        private final long id;
        private final VMRunnable runnable;
        private final long delayHeartbeats;
        private final long loopHeartBeats;
        private final boolean async;

        private boolean hasRun;
        private long exceedHeartbeats;
        private Future<?> future;

        public RegisteredTask(long id, VMRunnable runnable, long delayHeartbeats, long loopHeartBeats, boolean async)
        {
            this.id = id;
            this.runnable = runnable;
            this.delayHeartbeats = delayHeartbeats;
            this.loopHeartBeats = loopHeartBeats;
            this.exceedHeartbeats = 0;
            this.async = async;
        }
    }
}
