package tokyo.peya.langjal.vm.engine;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.scheduler.TaskScheduler;
import tokyo.peya.langjal.vm.engine.threading.VMMainThread;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.tracing.VMThreadTracer;

@Getter
public class VMEngine implements VMComponent
{
    @Getter(AccessLevel.NONE)
    private final JalVM vm;
    private final TaskScheduler scheduler;
    private final VMThreadGroup systemThreadGroup;

    private final VMMainThread mainThread;

    private long lastThreadID = 3;  // デフォルトの最初の ID が 3 なので， 3 から始める

    public VMEngine(@NotNull JalVM vm)
    {
        this.vm = vm;

        this.systemThreadGroup = new VMThreadGroup(vm);
        this.scheduler = new TaskScheduler(4);
        this.mainThread = new VMMainThread(vm, this.systemThreadGroup);

        this.systemThreadGroup.addThread(this.mainThread);
    }

    public boolean isRunning()
    {
        return this.systemThreadGroup.isRunning();
    }

    public VMThreadTracer getTracer()
    {
        return this.systemThreadGroup.getTracer();
    }

    public void startEngine()
    {
        while (this.isRunning())
        {
            this.systemThreadGroup.heartbeat();
            this.scheduler.heartbeat();
        }
    }

    @Override
    public @NotNull JalVM getVM()
    {
        return this.vm;
    }

    public VMThread getCurrentThread()
    {
        return this.systemThreadGroup.getCurrentHeartBeatingThread();
    }

    public long incrementAndGetThreadID()
    {
        return ++this.lastThreadID;
    }
}
