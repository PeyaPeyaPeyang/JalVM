package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMThreadGroup;

@Getter
public class VMThreadGroupHeartbeatEvent extends VMThreadGroupEvent
{
    public static final VMEventHandlerList<VMThreadGroupHeartbeatEvent> HANDLER_LIST = new VMEventHandlerList<>(
            VMThreadGroupHeartbeatEvent.class);

    public VMThreadGroupHeartbeatEvent(@NotNull JalVM vm, @NotNull VMThreadGroup group)
    {
        super(vm, group);
    }
}
