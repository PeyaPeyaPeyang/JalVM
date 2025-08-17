package tokyo.peya.langjal.vm.engine.stacking.instructions.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMInteger;

public class OperatorIConstSupport
{
    public static void execute(@NotNull VMFrame frame, @NotNull InsnNode operand, int i)
    {
        VMInteger vmInteger = new VMInteger(i);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(vmInteger, frame.getMethod(), operand)
        );

        frame.getStack().push(vmInteger);
    }
}
