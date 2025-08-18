package tokyo.peya.langjal.vm.engine.stacking.instructions.control;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMDouble;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorDReturn extends AbstractInstructionOperator<InsnNode>
{
    public OperatorDReturn()
    {
        super(EOpcodes.DRETURN, "dreturn");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMDouble value = frame.getStack().popType(VMType.DOUBLE);
        frame.returnFromMethod(value, operand);
    }
}
