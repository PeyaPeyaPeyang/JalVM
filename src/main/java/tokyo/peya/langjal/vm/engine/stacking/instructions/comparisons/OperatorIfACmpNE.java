package tokyo.peya.langjal.vm.engine.stacking.instructions.comparisons;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.JumpInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorIfACmpNE extends AbstractInstructionOperator<JumpInsnNode>
{

    public OperatorIfACmpNE()
    {
        super(EOpcodes.IF_ACMPNE, "if_acmpne");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull JumpInsnNode operand)
    {
        VMReferenceValue value1 = frame.getStack().popType(VMType.ofGenericObject(frame));
        VMReferenceValue value2 = frame.getStack().popType(VMType.ofGenericObject(frame));

        if (!value1.equals(value2))
            frame.jumpTo(operand.label.getLabel(), operand);
    }
}
