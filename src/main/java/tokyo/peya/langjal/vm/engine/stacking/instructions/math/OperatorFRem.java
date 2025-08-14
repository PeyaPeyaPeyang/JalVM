package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMFloat;

public class OperatorFRem extends AbstractInstructionOperator<InsnNode> {
    public OperatorFRem() {
        super(EOpcodes.FREM, "frem");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMFloat val1 = frame.getStack().popType(VMFloat.class);
        VMFloat val2 = frame.getStack().popType(VMFloat.class);
        VMFloat result = val2.rem(val1);
        frame.getTracer().pushHistory(
                ValueTracingEntry.combination(result, frame.getMethod(), operand, val2, val1)
        );
        frame.getStack().push(result);
    }
}
