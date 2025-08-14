package tokyo.peya.langjal.vm.api.events;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

public class VMThreadHeartbeatEvent extends VMThreadEvent{
    public static final VMEventHandlerList<VMThreadHeartbeatEvent> HANDLER_LIST = new VMEventHandlerList<>(VMThreadHeartbeatEvent.class);

    public VMThreadHeartbeatEvent(@NotNull JalVM vm, @NotNull VMThread thread) {
        super(vm, thread);
    }
}
