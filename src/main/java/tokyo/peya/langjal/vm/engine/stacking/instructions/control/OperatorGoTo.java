package tokyo.peya.langjal.vm.engine.stacking.instructions.control;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.JumpInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorGoTo extends AbstractInstructionOperator<JumpInsnNode>
{

    public OperatorGoTo()
    {
        super(EOpcodes.GOTO, "goto");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull JumpInsnNode operand)
    {
        frame.jumpTo(operand.label.getLabel(), operand);
    }
}
