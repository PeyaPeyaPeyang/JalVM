package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMDouble;

public class OperatorDRem extends AbstractInstructionOperator<InsnNode> {
    public OperatorDRem() {
        super(EOpcodes.DREM, "drem");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMDouble val1 = frame.getStack().popType(VMDouble.class);
        VMDouble val2 = frame.getStack().popType(VMDouble.class);
        VMDouble result = val2.rem(val1);
        frame.getTracer().pushHistory(
                ValueTracingEntry.combination(result, frame.getMethod(), operand, val2, val1)
        );
        frame.getStack().push(result);
    }
}
