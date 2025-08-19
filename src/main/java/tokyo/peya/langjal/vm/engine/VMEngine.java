package tokyo.peya.langjal.vm.engine;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.events.VMThreadDeathEvent;
import tokyo.peya.langjal.vm.api.events.VMThreadHeartbeatEvent;
import tokyo.peya.langjal.vm.api.events.VMThreadStartEvent;
import tokyo.peya.langjal.vm.engine.threads.VMMainThread;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.tracing.ThreadManipulationType;
import tokyo.peya.langjal.vm.tracing.ThreadTracingEntry;
import tokyo.peya.langjal.vm.tracing.VMThreadTracer;

import java.util.ArrayList;
import java.util.List;

@Getter
public class VMEngine
{
    private final JalVM vm;

    private final VMThreadTracer tracer;
    private final VMMainThread mainThread;
    @Getter(AccessLevel.NONE)
    private final List<VMThread> threads;

    private VMThread currentThread;

    public VMEngine(@NotNull JalVM vm)
    {
        this.vm = vm;
        this.mainThread = new VMMainThread(vm);
        this.tracer = new VMThreadTracer();
        this.threads = new ArrayList<>();

        this.addThread(this.mainThread);
        this.currentThread = this.mainThread;
    }

    public boolean isRunning()
    {
        return !this.threads.isEmpty();
    }

    public void startEngine()
    {
        while (this.isRunning())
            this.heartbeatThreads();
    }

    public void heartbeatThreads()
    {
        List<VMThread> deadThreads = new ArrayList<>();
        for (VMThread thread : this.threads)
        {
            this.currentThread = thread;
            if (thread.isAlive())
            {
                this.getVm().getEventManager().dispatchEvent(new VMThreadHeartbeatEvent(this.vm, thread));
                try
                {
                    thread.heartbeat();
                }
                catch (Throwable e)
                {
                    System.err.println("Error in thread " + thread.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    thread.kill(); // エラーが発生した場合はスレッドを終了
                    deadThreads.add(thread);
                }

            }
            else
            {
                System.out.println("Thread " + thread.getName() + " is dead, marking for removal.");
                deadThreads.add(thread);
            }
        }

        this.currentThread = null;

        for (VMThread deadThread : deadThreads)
        {
            System.out.println("Dead thread wiped out: " + deadThread.getName());
            this.killThread(deadThread);
        }
    }

    public void addThread(@NotNull VMThread thread)
    {
        if (this.threads.contains(thread))
            throw new IllegalStateException("Thread already exists in the engine.");

        this.getVm().getEventManager().dispatchEvent(new VMThreadStartEvent(this.getVm(), thread));

        this.threads.add(thread);
        this.tracer.pushHistory(
                new ThreadTracingEntry(
                        ThreadManipulationType.CREATION,
                        thread
                )
        );
    }

    public void killThread(@NotNull VMThread thread)
    {
        if (!this.threads.contains(thread))
            throw new IllegalStateException("Thread does not exist in the engine.");
        this.threads.remove(thread);
        thread.kill();
        this.tracer.pushHistory(
                new ThreadTracingEntry(
                        ThreadManipulationType.DESTRUCTION,
                        thread
                )
        );

        this.getVm().getEventManager().dispatchEvent(new VMThreadDeathEvent(this.getVm(), thread));
    }
}
