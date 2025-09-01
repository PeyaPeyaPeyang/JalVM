package tokyo.peya.langjal.vm.engine.stacking.instructions.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.IntInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.panics.IllegalOperandPanic;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMShort;

public class OperatorSIPush extends AbstractInstructionOperator<IntInsnNode>
{
    public OperatorSIPush()
    {
        super(EOpcodes.SIPUSH, "sipush");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull IntInsnNode operand)
    {
        int value = operand.operand;
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE)
            throw new IllegalOperandPanic("Value out of range for short: " + value);
        VMShort vmShort = new VMShort(frame, (short) value);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(vmShort, frame.getMethod(), operand)
        );

        frame.getStack().push(vmShort);
    }
}
