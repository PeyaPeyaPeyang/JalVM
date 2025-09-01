package tokyo.peya.langjal.vm.engine.stacking.instructions.comparisons;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorLookupSwitch extends AbstractInstructionOperator<LookupSwitchInsnNode>
{

    public OperatorLookupSwitch()
    {
        super(EOpcodes.LOOKUPSWITCH, "lookupswitch");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull LookupSwitchInsnNode operand)
    {
        VMInteger value = frame.getStack().popType(VMType.of(frame, PrimitiveTypes.INT));
        int intValue = value.asNumber().intValue();
        LabelNode target = null;
        for (int i = 0; i < operand.keys.size(); i++)
        {
            if (operand.keys.get(i) == intValue)
            {
                target = operand.labels.get(i);
                break;
            }
        }
        if (target == null)
            target = operand.dflt;

        frame.jumpTo(target.getLabel(), operand);
    }
}
