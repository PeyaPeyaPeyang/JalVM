package tokyo.peya.langjal.vm.api.events;


import lombok.Getter;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMFrame;

@Getter
public class VMFrameInEvent extends VMFrameEvent {
    public static final VMEventHandlerList<VMFrameInEvent> HANDLER_LIST = new VMEventHandlerList<>(VMFrameInEvent.class);

    public VMFrameInEvent(final JalVM vm, final VMFrame frame) {
        super(vm, frame);
    }
}
