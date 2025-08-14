package tokyo.peya.langjal.vm.engine.stacking.instructions.loads;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMInteger;

public class OperatorILoad extends AbstractInstructionOperator<VarInsnNode> {
    public OperatorILoad() {
        super(EOpcodes.ILOAD, "iload");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull VarInsnNode operand) {
        VMInteger val1 = frame.getLocals().getType(operand.var, VMInteger.class, operand);
        frame.getStack().push(val1);
    }
}
