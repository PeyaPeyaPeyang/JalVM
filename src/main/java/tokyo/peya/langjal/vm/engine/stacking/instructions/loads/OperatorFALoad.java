package tokyo.peya.langjal.vm.engine.stacking.instructions.loads;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.values.VMChar;
import tokyo.peya.langjal.vm.values.VMFloat;

public class OperatorFALoad extends AbstractInstructionOperator<InsnNode> {
    public OperatorFALoad() {
        super(EOpcodes.FALOAD, "faload");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull InsnNode operand) {
        OperatorXALoadSupporter.execute(frame, operand, VMFloat.class);
    }
}
