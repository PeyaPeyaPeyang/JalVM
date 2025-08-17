package tokyo.peya.langjal.vm.engine.stacking.instructions.loads;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMDouble;

public class OperatorDALoad extends AbstractInstructionOperator<InsnNode>
{
    public OperatorDALoad()
    {
        super(EOpcodes.DALOAD, "daload");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        OperatorXALoadSupporter.execute(frame, operand, VMDouble.class);
    }
}
