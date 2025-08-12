package tokyo.peya.langjal.vm.engine.stacking;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.*;
import tokyo.peya.langjal.vm.engine.stacking.instructions.OperatorSIPush;
import tokyo.peya.langjal.vm.exceptions.UnknownInstructionPanic;

public class VMStackMachine {
    private static final AbstractInstructionOperator <?>[] OPERATORS = new AbstractInstructionOperator<?>[]{
            // <editor-fold desc="Stack Manipulation">
            new OperatorSIPush(), // 0x11 - 17

            new OperatorIAdd(), // 0x60 - 96
            new OperatorLAdd(), // 0x61 - 97
            new OperatorFAdd(), // 0x62 - 98
            new OperatorDAdd(), // 0x63 - 99
            new OperatorISub(), // 0x64 - 100
            new OperatorLSub(), // 0x65 - 101
            new OperatorFSub(), // 0x66 - 102
            new OperatorDSub(), // 0x67 - 103
            new OperatorIMul(), // 0x68 - 104
            new OperatorLMul(), // 0x69 - 105
            new OperatorFMul(), // 0x6A - 106
            new OperatorDMul(), // 0x6B - 107
            new OperatorIDiv(), // 0x6C - 108
            new OperatorLDiv(), // 0x6D - 109
            new OperatorFDiv(), // 0x6E - 110
            new OperatorDDiv(), // 0x6F - 111
            new OperatorIRem(), // 0x70 - 112
            new OperatorLRem(), // 0x71 - 113
            new OperatorFRem(), // 0x72 - 114
            new OperatorDRem(), // 0x73 - 115
            new OperatorINeg(), // 0x74 - 116
            new OperatorLNeg(), // 0x75 - 117
            new OperatorFNeg(), // 0x76 - 118
            new OperatorDNeg(), // 0x77 - 119
            new OperatorIShl(), // 0x78 - 120
            new OperatorLShl(), // 0x79 - 121
            new OperatorIShr(), // 0x7A - 122
            new OperatorLShr(), // 0x7B - 123
            new OperatorIUShr(), // 0x7C - 124
            new OperatorLUShr(), // 0x7D - 125
            new OperatorIAnd(), // 0x7E - 126
            new OperatorLAnd(), // 0x7F - 127
            new OperatorIOr(), // 0x80 - 128
            new OperatorLOr(), // 0x81 - 129
            new OperatorIXor(), // 0x82 - 130
            new OperatorLXor(), // 0x83 - 131
            new OperatorIInc(), // 0x85 - 133


            // </editor-fold>
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
