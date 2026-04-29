package tokyo.peya.langjal.vm.api.events;

import tokyo.peya.langjal.vm.api.VMEvent;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;

public class VMStartupEvent extends VMEvent
{
    public static final VMEventHandlerList<VMStartupEvent> HANDLER_LIST = new VMEventHandlerList<>(VMStartupEvent.class);
}
