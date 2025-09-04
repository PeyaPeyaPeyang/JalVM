package tokyo.peya.langjal.vm.engine.scheduler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMEngine;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.VMThreadGroup;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.tracing.VMThreadTracer;
import tokyo.peya.langjal.vm.values.metaobjects.VMThreadGroupObject;

@Getter
public abstract class VMRunnable
{
    private long taskID = -1;

    private final JalVM vm;
    private final TaskScheduler scheduler;

    public abstract void run();

    private VMRunnable(TaskScheduler scheduler, JalVM vm)
    {
        this.scheduler = scheduler;
        this.vm = vm;
    }

    public void runTask()
    {
        this.scheduler.submitTask(this, 0, 0, false);
    }

    public void cancel()
    {
        if (this.taskID == -1)
            throw new IllegalStateException("Task ID is not set, cannot cancel.");
        this.scheduler.cancelTask(this.taskID);
    }

    public void runTaskAsynchronously()
    {
        this.scheduler.submitTask(this, 0, 0, true);
    }

    public void runTaskLater(long delay)
    {
        this.scheduler.submitTask(this, delay, 0, false);
    }

    public void runTaskLaterAsynchronously(long delay)
    {
        this.scheduler.submitTask(this, delay, 0, true);
    }

    public void runTaskTimer(long delay, long period)
    {
        this.scheduler.submitTask(this, delay, period, false);
    }

    public void runTaskTimerAsynchronously(long delay, long period)
    {
        this.scheduler.submitTask(this, delay, period, true);
    }

    public void runTaskTimerAsynchronously(long period)
    {
        this.scheduler.submitTask(this, 0, period, true);
    }

    @NotNull
    public static VMRunnable of(@NotNull VMComponent c, @NotNull VMFrame frame, @NotNull Runnable r)
    {
        return of(c, frame.getScheduler(), r);
    }

    @NotNull
    public static VMRunnable of(@NotNull VMComponent c, @NotNull VMThread thread, @NotNull Runnable r)
    {
        return of(c, thread.getScheduler(), r);
    }
    @NotNull
    public static VMRunnable of(@NotNull VMComponent c, @NotNull Runnable r)
    {
        return of(c, c.getVM().getEngine().getScheduler(), r);
    }


    @NotNull
    public static VMRunnable of(@NotNull VMComponent c, @NotNull TaskScheduler scheduler, @NotNull Runnable r)
    {
        return new VMRunnable(scheduler, c.getVM())
        {
            @Override
            public void run()
            {
                r.run();
            }
        };
    }

    public void setTaskID(long taskID)
    {
        if (this.taskID != -1)
            throw new IllegalStateException("Task ID is already set.");
        this.taskID = taskID;
    }
}
