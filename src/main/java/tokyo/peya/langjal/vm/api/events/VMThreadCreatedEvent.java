package tokyo.peya.langjal.vm.api.events;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.threading.VMThread;

public class VMThreadCreatedEvent extends VMThreadEvent
{
    public static final VMEventHandlerList<VMThreadCreatedEvent> HANDLER_LIST = new VMEventHandlerList<>(
            VMThreadCreatedEvent.class);

    public VMThreadCreatedEvent(@NotNull JalVM vm, @NotNull VMThread thread)
    {
        super(vm, thread);
    }
}
