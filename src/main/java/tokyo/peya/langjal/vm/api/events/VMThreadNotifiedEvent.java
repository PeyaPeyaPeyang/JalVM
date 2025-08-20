package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.threading.VMMonitor;
import tokyo.peya.langjal.vm.engine.threading.VMThread;

import java.time.Duration;

@Getter
public class VMThreadNotifiedEvent extends VMThreadEvent
{
    public static final VMEventHandlerList<VMThreadNotifiedEvent> HANDLER_LIST = new VMEventHandlerList<>(
            VMThreadNotifiedEvent.class);

    private final VMMonitor monitor;

    public VMThreadNotifiedEvent(@NotNull JalVM vm, @NotNull VMThread thread, @NotNull VMMonitor monitor)
    {
        super(vm, thread);
        this.monitor = monitor;
    }
}
