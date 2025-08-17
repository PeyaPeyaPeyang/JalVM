package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMFrame;

@Getter
public abstract class VMFrameEvent extends JalVMEvent
{
    public static final VMEventHandlerList<VMFrameEvent> HANDLER_LIST = new VMEventHandlerList<>(VMFrameEvent.class);
    private final VMFrame frame;

    public VMFrameEvent(@NotNull JalVM vm, @NotNull VMFrame frame)
    {
        super(vm);
        this.frame = frame;
    }
}
