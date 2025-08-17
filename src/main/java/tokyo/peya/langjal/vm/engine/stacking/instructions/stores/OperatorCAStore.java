package tokyo.peya.langjal.vm.engine.stacking.instructions.stores;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMChar;

public class OperatorCAStore extends AbstractInstructionOperator<InsnNode>
{
    public OperatorCAStore()
    {
        super(EOpcodes.CASTORE, "castore");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        OperatorXAStoreSupporter.execute(frame, operand, VMChar.class);
    }
}
