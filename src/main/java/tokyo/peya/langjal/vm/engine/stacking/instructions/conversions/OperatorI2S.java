package tokyo.peya.langjal.vm.engine.stacking.instructions.conversions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMChar;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMShort;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorI2S extends AbstractInstructionOperator<InsnNode>
{

    public OperatorI2S()
    {
        super(EOpcodes.I2S, "i2s");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMInteger value = frame.getStack().popType(VMType.INTEGER);
        VMShort result = new VMShort(value.asNumber().shortValue());
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
