package tokyo.peya.langjal.vm.engine.stacking.instructions.conversions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMByte;
import tokyo.peya.langjal.vm.values.VMChar;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorI2C extends AbstractInstructionOperator<InsnNode>
{

    public OperatorI2C()
    {
        super(EOpcodes.I2C, "i2c");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMInteger value = frame.getStack().popType(VMType.INTEGER);
        VMChar result = new VMChar((char) value.asNumber().intValue());
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
