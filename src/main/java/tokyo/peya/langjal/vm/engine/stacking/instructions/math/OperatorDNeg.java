package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMDouble;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorDNeg extends AbstractInstructionOperator<InsnNode>
{
    public OperatorDNeg()
    {
        super(EOpcodes.DNEG, "dneg");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMDouble val1 = frame.getStack().popType(VMType.DOUBLE);
        VMDouble result = val1.neg();
        frame.getTracer().pushHistory(
                ValueTracingEntry.manipulation(result, val1, frame.getMethod(), operand)
        );
        frame.getStack().push(result);
    }
}
