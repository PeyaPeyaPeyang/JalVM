package tokyo.peya.langjal.vm.engine.stacking.instructions.control;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;

public class OperatorReturn extends AbstractInstructionOperator<InsnNode>
{
    public OperatorReturn()
    {
        super(EOpcodes.RETURN, "return");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        frame.returnFromMethod();
    }
}
