package tokyo.peya.langjal.vm.engine.stacking.instructions.control;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMFloat;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorFReturn extends AbstractInstructionOperator<InsnNode>
{
    public OperatorFReturn()
    {
        super(EOpcodes.FRETURN, "freturn");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMFloat value = frame.getStack().popType(VMType.FLOAT);
        frame.returnFromMethod(value, operand);
    }
}
