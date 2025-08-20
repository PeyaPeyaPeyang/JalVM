package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.threading.VMMonitor;
import tokyo.peya.langjal.vm.engine.threading.VMThread;

import java.time.Duration;
import java.time.Instant;

@Getter
public class VMThreadWaitingEvent extends VMThreadEvent
{
    public static final VMEventHandlerList<VMThreadWaitingEvent> HANDLER_LIST = new VMEventHandlerList<>(
            VMThreadWaitingEvent.class);

    private final VMMonitor monitor;
    // ZERO to indicate indefinite waiting
    private final Duration waitDuration;

    public VMThreadWaitingEvent(@NotNull JalVM vm, @NotNull VMThread thread, @NotNull VMMonitor monitor,
                                @NotNull Duration waitDuration)
    {
        super(vm, thread);
        this.monitor = monitor;
        this.waitDuration = waitDuration;
    }
}
