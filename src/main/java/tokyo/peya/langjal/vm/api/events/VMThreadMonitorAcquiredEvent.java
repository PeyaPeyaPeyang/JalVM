package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.threading.VMMonitor;
import tokyo.peya.langjal.vm.engine.threading.VMThread;

@Getter
public class VMThreadMonitorAcquiredEvent extends VMThreadEvent
{
    public static final VMEventHandlerList<VMThreadMonitorAcquiredEvent> HANDLER_LIST = new VMEventHandlerList<>(
            VMThreadMonitorAcquiredEvent.class);

    private final VMMonitor monitor;

    public VMThreadMonitorAcquiredEvent(@NotNull JalVM vm, @NotNull VMThread thread, @NotNull VMMonitor monitor)
    {
        super(vm, thread);
        this.monitor = monitor;
    }
}
