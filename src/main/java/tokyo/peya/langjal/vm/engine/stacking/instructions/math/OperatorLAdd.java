package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorLAdd extends AbstractInstructionOperator<InsnNode>
{
    public OperatorLAdd()
    {
        super(EOpcodes.LADD, "ladd");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMLong val1 = frame.getStack().popType(VMType.LONG);
        VMLong val2 = frame.getStack().popType(VMType.LONG);
        VMLong result = val2.add(val1);
        frame.getTracer().pushHistory(
                ValueTracingEntry.combination(result, frame.getMethod(), operand, val1, val2)
        );
        frame.getStack().push(result);
    }
}
