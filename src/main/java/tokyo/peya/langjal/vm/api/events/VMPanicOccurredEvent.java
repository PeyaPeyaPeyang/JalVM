package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.panics.VMPanic;

@Getter
public class VMPanicOccurredEvent extends VMFrameEvent
{
    public static final VMEventHandlerList<VMPanicOccurredEvent> HANDLER_LIST = new VMEventHandlerList<>(
            VMPanicOccurredEvent.class);

    private final VMPanic panic;

    public VMPanicOccurredEvent(@NotNull VMFrame frame, @NotNull VMPanic panic)
    {
        super(frame.getVM(), frame);
        this.panic = panic;
    }
}
