package tokyo.peya.langjal.vm.engine.stacking.instructions.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMLong;

public class OperatorLConst0 extends AbstractInstructionOperator<InsnNode>
{
    public OperatorLConst0()
    {
        super(EOpcodes.LCONST_0, "lconst_0");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand)
    {
        VMLong value = new VMLong(0L);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(value, frame.getMethod(), operand)
        );

        frame.getStack().push(value);
    }
}
