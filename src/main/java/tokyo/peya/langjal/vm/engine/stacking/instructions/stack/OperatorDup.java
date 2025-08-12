package tokyo.peya.langjal.vm.engine.stacking.instructions.stack;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.IllegalOperationPanic;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorDup extends AbstractInstructionOperator<InsnNode> {
    public OperatorDup() {
        super(EOpcodes.DUP, "dup");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMValue value = frame.getStack().peek();
        if (value.isCategory2())
            throw new IllegalOperationPanic("Cannot duplicate category 2 value with DUP");

        frame.getStack().push(value);
    }
}
