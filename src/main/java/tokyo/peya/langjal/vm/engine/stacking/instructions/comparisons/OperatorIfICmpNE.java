package tokyo.peya.langjal.vm.engine.stacking.instructions.comparisons;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.JumpInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorIfICmpNE extends AbstractInstructionOperator<JumpInsnNode>
{

    public OperatorIfICmpNE()
    {
        super(EOpcodes.IF_ICMPNE, "if_icmpne");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull JumpInsnNode operand)
    {
        VMInteger value1 = frame.getStack().popType(VMType.INTEGER);
        VMInteger value2 = frame.getStack().popType(VMType.INTEGER);
        int intValue1 = value1.asNumber().intValue();
        int intValue2 = value2.asNumber().intValue();
        if (intValue1 != intValue2)
            frame.jumpTo(operand.label.getLabel(), operand);
    }
}
