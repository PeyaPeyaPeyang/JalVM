package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMInteger;

public class OperatorIDiv extends AbstractInstructionOperator<InsnNode> {
    public OperatorIDiv() {
        super(EOpcodes.IMUL, "imul");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMInteger val1 = frame.getStack().popType(VMInteger.class);
        VMInteger val2 = frame.getStack().popType(VMInteger.class);

        frame.getStack().push(val2.mul(val1));
    }
}
