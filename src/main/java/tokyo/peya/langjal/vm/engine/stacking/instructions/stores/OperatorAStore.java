package tokyo.peya.langjal.vm.engine.stacking.instructions.stores;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.VarInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMReferenceValue;

public class OperatorAStore extends AbstractInstructionOperator<VarInsnNode>
{
    public OperatorAStore()
    {
        super(EOpcodes.ASTORE, "astore");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull VarInsnNode operand)
    {
        VMReferenceValue value = frame.getStack().popType(VMReferenceValue.class);
        frame.getLocals().setSlot(operand.var, value);
    }
}
