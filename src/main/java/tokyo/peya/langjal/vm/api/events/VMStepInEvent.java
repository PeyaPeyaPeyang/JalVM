package tokyo.peya.langjal.vm.api.events;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMFrame;

@Getter
public class VMStepInEvent extends VMFrameEvent
{
    public static final VMEventHandlerList<VMStepInEvent> HANDLER_LIST = new VMEventHandlerList<>(VMStepInEvent.class);
    private final AbstractInsnNode instruction;

    public VMStepInEvent(@NotNull VMFrame frame, @NotNull AbstractInsnNode instruction)
    {
        super(frame.getVM(), frame);
        this.instruction = instruction;
    }
}
