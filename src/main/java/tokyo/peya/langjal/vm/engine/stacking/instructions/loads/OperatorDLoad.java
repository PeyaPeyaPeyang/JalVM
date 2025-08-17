package tokyo.peya.langjal.vm.engine.stacking.instructions.loads;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.VarInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMDouble;

public class OperatorDLoad extends AbstractInstructionOperator<VarInsnNode>
{
    public OperatorDLoad()
    {
        super(EOpcodes.DLOAD, "dload");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull VarInsnNode operand)
    {
        VMDouble val1 = frame.getLocals().getType(operand.var, VMDouble.class, operand);
        frame.getStack().push(val1);
    }
}
