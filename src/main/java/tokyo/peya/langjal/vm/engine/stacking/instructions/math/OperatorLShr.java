package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMLong;

public class OperatorLShr extends AbstractInstructionOperator<InsnNode>
{
    public OperatorLShr()
    {
        super(EOpcodes.LSHR, "lshr");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMLong val1 = frame.getStack().popType(VMLong.class);
        VMLong val2 = frame.getStack().popType(VMLong.class);
        VMLong result = val2.shr(val1);
        frame.getTracer().pushHistory(
                ValueTracingEntry.combination(result, frame.getMethod(), operand, val2, val1)
        );
        frame.getStack().push(result);
    }
}
