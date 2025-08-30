package tokyo.peya.langjal.vm.engine.stacking.instructions.comparisons;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.JumpInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorIfGT extends AbstractInstructionOperator<JumpInsnNode>
{

    public OperatorIfGT()
    {
        super(EOpcodes.IFGT, "ifgt");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull JumpInsnNode operand)
    {
        VMInteger value = frame.getStack().popType(VMType.of(frame, PrimitiveTypes.INT));
        int intValue = value.asNumber().intValue();
        if (intValue > 0)
            frame.jumpTo(operand.label.getLabel(), operand);
    }
}
