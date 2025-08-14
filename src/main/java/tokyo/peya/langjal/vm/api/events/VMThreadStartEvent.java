package tokyo.peya.langjal.vm.api.events;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

public class VMThreadStartEvent extends VMThreadEvent {
    public static final VMEventHandlerList<VMThreadStartEvent> HANDLER_LIST = new VMEventHandlerList<>(VMThreadStartEvent.class);

    public VMThreadStartEvent(@NotNull JalVM vm, @NotNull VMThread thread) {
        super(vm, thread);
    }
}
