package tokyo.peya.langjal.vm.engine.stacking.instructions.comparisons;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMInteger;

public class OperatorFCmpG extends AbstractInstructionOperator<InsnNode>
{

    public OperatorFCmpG()
    {
        super(EOpcodes.FCMPG, "fcmpg");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        NumericComparator.compareFloat(frame, new VMInteger(frame, 1));
    }
}
