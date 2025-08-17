package tokyo.peya.langjal.vm.engine.stacking.instructions.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMFloat;

public class OperatorFConst0 extends AbstractInstructionOperator<InsnNode>
{
    public OperatorFConst0()
    {
        super(EOpcodes.FCONST_0, "fconst_0");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMFloat value = new VMFloat(0.0f);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(value, frame.getMethod(), operand)
        );

        frame.getStack().push(value);
    }
}
