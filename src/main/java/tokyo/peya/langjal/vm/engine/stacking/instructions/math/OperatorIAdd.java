package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMInteger;

public class OperatorIAdd extends AbstractInstructionOperator<InsnNode> {
    public OperatorIAdd() {
        super(EOpcodes.IADD, "iadd");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMInteger i1 = frame.getStack().popType(VMInteger.class);
        VMInteger i2 = frame.getStack().popType(VMInteger.class);

        frame.getStack().push(i2.add(i1));
    }
}
