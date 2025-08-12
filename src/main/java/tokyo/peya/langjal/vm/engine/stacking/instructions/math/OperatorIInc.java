package tokyo.peya.langjal.vm.engine.stacking.instructions.math;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.IincInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMInteger;

public class OperatorIInc extends AbstractInstructionOperator<IincInsnNode> {
    public OperatorIInc() {
        super(EOpcodes.IINC, "iinc");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull IincInsnNode operand) {
        VMInteger val1 = frame.getLocals().getType(operand.var, VMInteger.class);
        int amount = operand.incr;

        if (amount == 0) {
            return; // No increment needed
        }
        VMInteger incrementedValue = val1.add(new VMInteger(amount));
        frame.getLocals().setSlot(operand.var, incrementedValue);
    }
}
