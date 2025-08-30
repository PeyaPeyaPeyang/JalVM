package tokyo.peya.langjal.vm.api.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.api.VMEvent;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;

@AllArgsConstructor
public abstract class JalVMEvent extends VMEvent
{
    public static final VMEventHandlerList<JalVMEvent> HANDLER_LIST = new VMEventHandlerList<>(JalVMEvent.class);

    private final JalVM vm;

    public JalVM getVM() { return this.vm; }
}
