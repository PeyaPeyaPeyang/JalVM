package tokyo.peya.langjal.vm.engine.stacking.instructions.loads;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.VarInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMFloat;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorFLoad extends AbstractInstructionOperator<VarInsnNode>
{
    public OperatorFLoad()
    {
        super(EOpcodes.FLOAD, "fload");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull VarInsnNode operand)
    {
        VMFloat val1 = frame.getLocals().getType(operand.var, VMType.FLOAT, operand);
        frame.getStack().push(val1);
    }
}
