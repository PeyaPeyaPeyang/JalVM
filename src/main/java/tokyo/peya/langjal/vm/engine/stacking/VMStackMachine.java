package tokyo.peya.langjal.vm.engine.stacking;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.engine.stacking.instructions.OperatorIAdd;
import tokyo.peya.langjal.vm.engine.stacking.instructions.OperatorSIPush;
import tokyo.peya.langjal.vm.exceptions.UnknownInstructionPanic;

public class VMStackMachine {
    private static final AbstractInstructionOperator <?>[] OPERATORS = new AbstractInstructionOperator<?>[]{
            new OperatorSIPush(), // 0x11 - 17

            new OperatorIAdd(), // 0x60 - 96
    };

    public static AbstractInstructionOperator<?> getOperator(final int opcode) {
        for (AbstractInstructionOperator<?> operator : OPERATORS)
            if (operator.getOpcode() == opcode)
                return operator;

        throw new UnknownInstructionPanic("No operator found for opcode: " + opcode);
    }

    @SuppressWarnings("unchecked")
    public static void executeInstruction(@NotNull VMFrame frame,@NotNull AbstractInsnNode insn) {
        AbstractInstructionOperator<AbstractInsnNode> operator = (AbstractInstructionOperator<AbstractInsnNode>) getOperator(insn.getOpcode());

        operator.execute(frame, insn);
    }
}
