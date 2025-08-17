package tokyo.peya.langjal.vm.engine.stacking.instructions;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.engine.stacking.InstructionExecutor;

@Getter
public abstract class AbstractInstructionOperator<T extends AbstractInsnNode> implements InstructionExecutor<T>
{
    private final int opcode;
    private final String name;

    protected AbstractInstructionOperator(int opcode, @NotNull String name)
    {
        this.opcode = opcode;
        this.name = name;
    }

}
