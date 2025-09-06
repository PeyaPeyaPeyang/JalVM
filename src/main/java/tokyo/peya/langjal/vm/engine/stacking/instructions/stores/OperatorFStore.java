package tokyo.peya.langjal.vm.engine.stacking.instructions.stores;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.VarInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMFloat;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorFStore extends AbstractInstructionOperator<VarInsnNode>
{
    public OperatorFStore()
    {
        super(EOpcodes.FSTORE, "fstore");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull VarInsnNode operand)
    {
        VMFloat value = frame.getStack().popType(VMType.of(frame, PrimitiveTypes.FLOAT));
        frame.getLocals().setSlot(operand.var, value, operand);
    }
}
