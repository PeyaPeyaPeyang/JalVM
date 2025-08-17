package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.IincInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMInteger;

public class OperatorIInc extends AbstractInstructionOperator<IincInsnNode>
{
    public OperatorIInc()
    {
        super(EOpcodes.IINC, "iinc");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull IincInsnNode operand)
    {
        VMInteger val1 = frame.getLocals().getType(operand.var, VMInteger.class, operand);
        int amount = operand.incr;

        if (amount == 0)
            return; // No increment needed
        VMInteger amountValue = new VMInteger(amount);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(amountValue, frame.getMethod(), operand)
        );
        VMInteger incrementedValue = val1.add(amountValue);
        frame.getTracer().pushHistory(
                ValueTracingEntry.combination(incrementedValue, frame.getMethod(), operand, val1, amountValue)
        );


        frame.getLocals().setSlot(operand.var, incrementedValue, operand);
    }
}
