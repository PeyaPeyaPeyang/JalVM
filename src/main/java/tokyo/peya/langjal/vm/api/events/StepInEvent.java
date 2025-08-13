package tokyo.peya.langjal.vm.api.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.api.VMEvent;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;

@Getter
@AllArgsConstructor
public class StepInEvent extends VMEvent {
    public static final VMEventHandlerList<StepInEvent> HANDLER_LIST = new VMEventHandlerList<>(StepInEvent.class);

    private final VMFrame frame;
    private final AbstractInsnNode instruction;
}
