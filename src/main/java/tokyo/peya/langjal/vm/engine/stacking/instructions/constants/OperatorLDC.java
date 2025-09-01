package tokyo.peya.langjal.vm.engine.stacking.instructions.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.LdcInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorLDC extends AbstractInstructionOperator<LdcInsnNode>
{
    public OperatorLDC()
    {
        super(EOpcodes.LDC, "ldc");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull LdcInsnNode operand)
    {
        Object value = operand.cst;
        VMValue toValue = VMValue.fromJavaObject(frame, value);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(toValue, frame.getMethod(), operand)
        );
        frame.getStack().push(toValue);
    }
}
