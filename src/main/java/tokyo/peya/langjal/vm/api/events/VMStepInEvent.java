package tokyo.peya.langjal.vm.api.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.api.VMEvent;
import tokyo.peya.langjal.vm.api.VMEventHandlerList;
import tokyo.peya.langjal.vm.engine.VMFrame;

@Getter
public class VMStepInEvent extends VMFrameEvent {
    public static final VMEventHandlerList<VMStepInEvent> HANDLER_LIST = new VMEventHandlerList<>(VMStepInEvent.class);

    public VMStepInEvent(@NotNull VMFrame frame, @NotNull AbstractInsnNode instruction) {
        super(frame.getVm(), frame);
        this.instruction = instruction;
    }

    private final AbstractInsnNode instruction;
}
