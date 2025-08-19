package tokyo.peya.langjal.vm.engine.stacking.instructions.conversions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMByte;
import tokyo.peya.langjal.vm.values.VMDouble;
import tokyo.peya.langjal.vm.values.VMFloat;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorI2B extends AbstractInstructionOperator<InsnNode>
{

    public OperatorI2B()
    {
        super(EOpcodes.I2B, "i2b");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMInteger value = frame.getStack().popType(VMType.INTEGER);
        VMByte result = new VMByte(value.asNumber().byteValue());
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
