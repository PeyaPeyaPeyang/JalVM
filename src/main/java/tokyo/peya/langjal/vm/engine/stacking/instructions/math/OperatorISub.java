package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMFloat;

public class OperatorISub extends AbstractInstructionOperator<InsnNode> {
    public OperatorISub() {
        super(EOpcodes.LADD, "ladd");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMFloat l1 = frame.getStack().popType(VMFloat.class);
        VMFloat l2 = frame.getStack().popType(VMFloat.class);

        frame.getStack().push(l1.add(l2));
    }
}
