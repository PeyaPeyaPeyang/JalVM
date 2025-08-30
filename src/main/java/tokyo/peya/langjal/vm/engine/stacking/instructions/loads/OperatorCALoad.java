package tokyo.peya.langjal.vm.engine.stacking.instructions.loads;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMChar;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorCALoad extends AbstractInstructionOperator<InsnNode>
{
    public OperatorCALoad()
    {
        super(EOpcodes.CALOAD, "caload");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        OperatorXALoadSupporter.execute(frame, operand, VMType.of(frame, PrimitiveTypes.CHAR));
    }
}
