package tokyo.peya.langjal.vm.engine.stacking.instructions.comparisons;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorTableSwitch extends AbstractInstructionOperator<TableSwitchInsnNode>
{

    public OperatorTableSwitch()
    {
        super(EOpcodes.TABLESWITCH, "tableswitch");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull TableSwitchInsnNode operand)
    {
        VMInteger value = frame.getStack().popType(VMType.INTEGER);
        int intValue = value.asNumber().intValue();

        int min = operand.min;
        int max = operand.max;
        LabelNode target = null;
        if (min <= intValue && intValue <= max)
        {
            int index = intValue - min;
            target = operand.labels.get(index);
        }

        if (target == null)
            target = operand.dflt;

        frame.jumpTo(target.getLabel(), operand);
    }
}
