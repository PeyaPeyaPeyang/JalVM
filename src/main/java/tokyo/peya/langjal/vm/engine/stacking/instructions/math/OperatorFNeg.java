package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMFloat;

public class OperatorFNeg extends AbstractInstructionOperator<InsnNode> {
    public OperatorFNeg() {
        super(EOpcodes.FADD, "fadd");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMFloat val1 = frame.getStack().popType(VMFloat.class);
        VMFloat val2 = frame.getStack().popType(VMFloat.class);

        frame.getStack().push(val2.add(val1));
    }
}
