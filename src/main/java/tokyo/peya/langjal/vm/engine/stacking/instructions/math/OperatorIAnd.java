package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorIAnd extends AbstractInstructionOperator<InsnNode>
{
    public OperatorIAnd()
    {
        super(EOpcodes.IAND, "iand");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMInteger val1 = frame.getStack().popType(VMType.INTEGER);
        VMInteger val2 = frame.getStack().popType(VMType.INTEGER);
        VMInteger result = val2.and(val1);
        frame.getTracer().pushHistory(
                ValueTracingEntry.combination(result, frame.getMethod(), operand, val1, val2)
        );
        frame.getStack().push(result);
    }
}
