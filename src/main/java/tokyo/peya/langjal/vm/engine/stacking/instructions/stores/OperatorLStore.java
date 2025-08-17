package tokyo.peya.langjal.vm.engine.stacking.instructions.stores;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.VarInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMLong;

public class OperatorLStore extends AbstractInstructionOperator<VarInsnNode>
{
    public OperatorLStore()
    {
        super(EOpcodes.LSTORE, "lstore");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull VarInsnNode operand)
    {
        VMLong val1 = frame.getLocals().getType(operand.var, VMLong.class, operand);
        frame.getStack().push(val1);
    }
}
