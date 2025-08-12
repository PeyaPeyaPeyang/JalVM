package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMDouble;
import tokyo.peya.langjal.vm.values.VMLong;

public class OperatorDAdd extends AbstractInstructionOperator<InsnNode> {
    public OperatorDAdd() {
        super(EOpcodes.DADD, "dadd");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMDouble val1 = frame.getStack().popType(VMDouble.class);
        VMDouble val2 = frame.getStack().popType(VMDouble.class);

        frame.getStack().push(val1.add(val2));
    }
}
