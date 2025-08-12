package tokyo.peya.langjal.vm.engine.stacking;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.engine.VMFrame;

public interface InstructionExecutor<T extends AbstractInsnNode> {
    void execute(@NotNull VMFrame frame, @NotNull T operand);

    int getOpcode();

    String getName();
}
