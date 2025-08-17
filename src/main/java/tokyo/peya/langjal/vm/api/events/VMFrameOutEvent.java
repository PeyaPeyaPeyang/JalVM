package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMFrame;

@Getter
public class VMFrameOutEvent extends VMFrameEvent
{
    public static final VMEventHandlerList<VMFrameOutEvent> HANDLER_LIST = new VMEventHandlerList<>(VMFrameOutEvent.class);

    private final VMFrame previousFrame;

    public VMFrameOutEvent(@NotNull JalVM vm, @NotNull VMFrame frame, @Nullable VMFrame previousFrame)
    {
        super(vm, frame);
        this.previousFrame = previousFrame;
    }
}
