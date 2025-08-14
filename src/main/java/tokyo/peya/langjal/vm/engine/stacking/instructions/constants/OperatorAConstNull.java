package tokyo.peya.langjal.vm.engine.stacking.instructions.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMNull;
import tokyo.peya.langjal.vm.values.VMShort;
import tokyo.peya.langjal.vm.values.VMType;

public class OperatorAConstNull extends AbstractInstructionOperator<InsnNode> {
    public OperatorAConstNull() {
        super(EOpcodes.ACONST_NULL, "aconst_null");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        VMNull vmNull = new VMNull(VMType.VOID);
        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(vmNull, frame.getMethod(), operand)
        );

        frame.getStack().push(vmNull);
    }
}
