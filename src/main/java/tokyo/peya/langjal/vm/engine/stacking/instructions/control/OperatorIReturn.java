package tokyo.peya.langjal.vm.engine.stacking.instructions.control;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorIReturn extends AbstractInstructionOperator<InsnNode>
{
    public OperatorIReturn()
    {
        super(EOpcodes.IRETURN, "ireturn");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMInteger value = frame.getStack().popType(VMType.INTEGER);
        frame.returnFromMethod(value, operand);
    }
}
