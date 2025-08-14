package tokyo.peya.langjal.vm.engine.stacking.instructions.stack;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.IllegalOperationPanic;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorDup2 extends AbstractInstructionOperator<InsnNode> {
    public OperatorDup2() {
        super(EOpcodes.DUP2, "dup2");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMStack stack = frame.getStack();

        VMValue value1 = stack.peek();
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(
                        value1,
                        frame.getMethod(),
                        operand
                )
        );
        if (value1.isCategory2()) {
            stack.push(value1);
        } else {
            /* value1 = */
            stack.pop();
            VMValue value2 = stack.pop();
            if (value2.isCategory2())
                throw new IllegalOperationPanic("Cannot duplicate category 2 value with DUP2");

            frame.getTracer().pushHistory(
                    ValueTracingEntry.generation(
                            value2,
                            frame.getMethod(),
                            operand
                    )
            );

            stack.push(value2);
            stack.push(value1);
            stack.push(value2);
            stack.push(value1);
        }
    }
}
