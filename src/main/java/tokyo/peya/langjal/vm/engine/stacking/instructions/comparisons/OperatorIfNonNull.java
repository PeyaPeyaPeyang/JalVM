package tokyo.peya.langjal.vm.engine.stacking.instructions.comparisons;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.JumpInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMNull;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorIfNonNull extends AbstractInstructionOperator<JumpInsnNode>
{

    public OperatorIfNonNull()
    {
        super(EOpcodes.IFNONNULL, "ifnonnull");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull JumpInsnNode operand)
    {
        VMReferenceValue value = frame.getStack().popType(VMType.ofGenericObject(frame));
        if (!(value instanceof VMNull))
            frame.jumpTo(operand.label.getLabel(), operand);
    }
}
