package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMLong;

public class OperatorLNeg extends AbstractInstructionOperator<InsnNode> {
    public OperatorLNeg() {
        super(EOpcodes.LNEG, "lneg");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMLong val1 = frame.getStack().popType(VMLong.class);
        VMLong result = val1.neg();
        frame.getTracer().pushHistory(
                ValueTracingEntry.manipulation(result, val1, frame.getMethod(), operand)
        );
        frame.getStack().push(result);
    }
}
