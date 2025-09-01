package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMThreadGroup;

@Getter
public abstract class VMThreadGroupEvent extends JalVMEvent
{
    public static final VMEventHandlerList<VMThreadGroupEvent> HANDLER_LIST = new VMEventHandlerList<>(VMThreadGroupEvent.class);

    private final VMThreadGroup group;

    public VMThreadGroupEvent(@NotNull JalVM vm, @NotNull VMThreadGroup group)
    {
        super(vm);
        this.group = group;
    }
}
