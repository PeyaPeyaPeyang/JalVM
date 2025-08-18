package tokyo.peya.langjal.vm.engine.stacking.instructions.control;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorAReturn extends AbstractInstructionOperator<InsnNode>
{
    public OperatorAReturn()
    {
        super(EOpcodes.ARETURN, "areturn");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMReferenceValue value = frame.getStack().popType(VMType.GENERIC_OBJECT);
        frame.returnFromMethod(value, operand);
    }
}
