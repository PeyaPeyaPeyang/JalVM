package tokyo.peya.langjal.vm.engine.stacking.instructions.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.IntInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMByte;

public class OperatorBIPush extends AbstractInstructionOperator<IntInsnNode>
{
    public OperatorBIPush()
    {
        super(EOpcodes.BIPUSH, "bipush");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull IntInsnNode operand)
    {
        int value = operand.operand;
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE)
            throw new IllegalOperandPanic("Value out of range for byte: " + value);
        VMByte vmShort = new VMByte(frame, (byte) value);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(vmShort, frame.getMethod(), operand)
        );

        frame.getStack().push(vmShort);
    }
}
