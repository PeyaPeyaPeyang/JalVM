package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.engine.threading.VMThreadState;

@Getter
public class VMThreadChangeStateEvent extends VMThreadEvent
{
    public static final VMEventHandlerList<VMThreadChangeStateEvent> HANDLER_LIST =
            new VMEventHandlerList<>(VMThreadChangeStateEvent.class);

    private final VMThreadState oldState;
    private final VMThreadState newState;

    public VMThreadChangeStateEvent(@NotNull JalVM vm, @NotNull VMThread thread, VMThreadState oldState, VMThreadState newState)
    {
        super(vm, thread);
        this.oldState = oldState;
        this.newState = newState;
    }
}
