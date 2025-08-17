package tokyo.peya.langjal.vm.engine.stacking.instructions.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;

public class OperatorIConst4 extends AbstractInstructionOperator<InsnNode>
{
    public OperatorIConst4()
    {
        super(EOpcodes.ICONST_4, "iconst_4");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        OperatorIConstSupport.execute(frame, operand, 4);
    }
}
