package tokyo.peya.langjal.vm.engine.stacking.instructions.conversions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMDouble;
import tokyo.peya.langjal.vm.values.VMFloat;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorD2F extends AbstractInstructionOperator<InsnNode>
{

    public OperatorD2F()
    {
        super(EOpcodes.D2F, "d2f");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMDouble value = frame.getStack().popType(VMType.DOUBLE);
        VMFloat result = new VMFloat(value.asNumber().floatValue());
        frame.getTracer().pushHistory(
                ValueTracingEntry.manipulation(
                        result,
                        value,
                        frame.getMethod(),
                        operand
                )
        );
        frame.getStack().push(result);
    }
}
