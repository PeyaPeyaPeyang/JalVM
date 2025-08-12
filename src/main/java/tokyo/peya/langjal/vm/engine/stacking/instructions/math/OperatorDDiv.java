package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMDouble;

public class OperatorDDiv extends AbstractInstructionOperator<InsnNode> {
    public OperatorDDiv() {
        super(EOpcodes.DSUB, "dsub");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMDouble val1 = frame.getStack().popType(VMDouble.class);
        VMDouble val2 = frame.getStack().popType(VMDouble.class);

        frame.getStack().push(val2.sub(val1));
    }
}
