package tokyo.peya.langjal.vm.engine.stacking.instructions.stack;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.IllegalOperationPanic;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorDupX1 extends AbstractInstructionOperator<InsnNode> {
    public OperatorDupX1() {
        super(EOpcodes.DUP_X1, "dup_x1");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMStack stack = frame.getStack();

        VMValue value1 = stack.pop();
        VMValue value2 = stack.pop();
        if (value1.isCategory2() || value2.isCategory2())
            throw new IllegalOperationPanic("Cannot duplicate category 2 value with DUP_X1");

        stack.push(value1);
        stack.push(value2);
        stack.push(value1);
    }
}
