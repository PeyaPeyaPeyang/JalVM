package tokyo.peya.langjal.vm.engine.stacking.instructions.loads;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMLong;

public class OperatorLALoad extends AbstractInstructionOperator<InsnNode>
{
    public OperatorLALoad()
    {
        super(EOpcodes.LALOAD, "laload");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        OperatorXALoadSupporter.execute(frame, operand, VMLong.class);
    }
}
