package tokyo.peya.langjal.vm.engine.stacking.instructions.stack;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.IllegalOperationPanic;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorDup2X2 extends AbstractInstructionOperator<InsnNode>
{
    public OperatorDup2X2()
    {
        super(EOpcodes.DUP2_X1, "dup2_x1");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMStack stack = frame.getStack();

        VMValue value1 = stack.pop();
        VMValue value2 = stack.pop();
        if (value1.isCategory2())
        {
            if (value2.isCategory2())
            {
                frame.getTracer().pushHistory(ValueTracingEntry.generation(value1, frame.getMethod(), operand));
                stack.push(value1);
                stack.push(value2);
                stack.push(value1);
                return;
            }
            VMValue value3 = stack.pop();
            if (value3.isCategory2())
                throw new IllegalOperationPanic("Cannot duplicate two category 2 values with DUP2_X2");
            frame.getTracer().pushHistory(ValueTracingEntry.generation(value1, frame.getMethod(), operand));
            frame.getTracer().pushHistory(ValueTracingEntry.generation(value2, frame.getMethod(), operand));

            stack.push(value1);
            stack.push(value2);
            stack.push(value3);
            stack.push(value2);
            stack.push(value1);
            return;
        }

        if (value2.isCategory2())
            throw new IllegalOperationPanic("Cannot duplicate category 2 value with DUP2_X2");

        frame.getTracer().pushHistory(ValueTracingEntry.generation(value2, frame.getMethod(), operand));
        frame.getTracer().pushHistory(ValueTracingEntry.generation(value1, frame.getMethod(), operand));

        VMValue value3 = stack.pop();
        if (value3.isCategory2())
        {
            stack.push(value2);
            stack.push(value1);
            stack.push(value3);
            stack.push(value2);
            stack.push(value1);
        }
        else
        {
            VMValue value4 = stack.pop();
            if (value4.isCategory2())
                throw new IllegalOperationPanic("Cannot duplicate category 2 value with DUP2_X2");

            stack.push(value2);
            stack.push(value1);
            stack.push(value4);
            stack.push(value3);
            stack.push(value2);
            stack.push(value1);
        }
    }
}
