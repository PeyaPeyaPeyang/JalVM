package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

@Getter
public abstract class VMThreadEvent extends JalVMEvent
{
    public static final VMEventHandlerList<VMThreadEvent> HANDLER_LIST = new VMEventHandlerList<>(VMThreadEvent.class);

    private final VMThread thread;

    public VMThreadEvent(@NotNull JalVM vm, @NotNull VMThread thread)
    {
        super(vm);
        this.thread = thread;
    }
}
