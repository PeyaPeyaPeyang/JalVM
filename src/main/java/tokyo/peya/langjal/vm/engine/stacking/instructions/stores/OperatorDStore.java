package tokyo.peya.langjal.vm.engine.stacking.instructions.stores;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.VarInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMDouble;
import tokyo.peya.langjal.vm.values.VMReferenceValue;

public class OperatorDStore extends AbstractInstructionOperator<VarInsnNode> {
    public OperatorDStore() {
        super(EOpcodes.DSTORE, "dstore");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull VarInsnNode operand) {
        VMDouble value = frame.getStack().popType(VMDouble.class);
        frame.getLocals().setSlot(operand.var, value);
    }
}
