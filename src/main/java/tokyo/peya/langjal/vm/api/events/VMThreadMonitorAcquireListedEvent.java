package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.threading.VMMonitor;
import tokyo.peya.langjal.vm.engine.threading.VMThread;

@Getter
public class VMThreadMonitorAcquireListedEvent extends VMThreadEvent
{
    public static final VMEventHandlerList<VMThreadMonitorAcquireListedEvent> HANDLER_LIST = new VMEventHandlerList<>(
            VMThreadMonitorAcquireListedEvent.class);

    private final VMMonitor monitor;
    private final int waiters;

    public VMThreadMonitorAcquireListedEvent(@NotNull JalVM vm, @NotNull VMThread thread, @NotNull VMMonitor monitor, int waiters)
    {
        super(vm, thread);
        this.monitor = monitor;
        this.waiters = waiters;
    }
}
