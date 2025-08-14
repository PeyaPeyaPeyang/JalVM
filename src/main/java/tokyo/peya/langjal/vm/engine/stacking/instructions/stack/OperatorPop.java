package tokyo.peya.langjal.vm.engine.stacking.instructions.stack;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorPop extends AbstractInstructionOperator<InsnNode> {
    public OperatorPop() {
        super(EOpcodes.POP, "pop");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMValue popped = frame.getStack().pop();
        frame.getTracer().pushHistory(ValueTracingEntry.destruction(
                popped,
                frame.getMethod(),
                operand
        ));
    }
}
