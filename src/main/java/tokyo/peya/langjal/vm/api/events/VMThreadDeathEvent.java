package tokyo.peya.langjal.vm.api.events;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

public class VMThreadDeathEvent extends VMThreadEvent {
    public static final VMEventHandlerList<VMThreadDeathEvent> HANDLER_LIST = new VMEventHandlerList<>(VMThreadDeathEvent.class);

    public VMThreadDeathEvent(@NotNull JalVM vm, @NotNull VMThread thread) {
        super(vm, thread);
    }
}
