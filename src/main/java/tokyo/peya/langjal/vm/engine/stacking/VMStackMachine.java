package tokyo.peya.langjal.vm.engine.stacking;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.engine.stacking.instructions.comparisons.OperatorIfNonNull;
import tokyo.peya.langjal.vm.engine.stacking.instructions.comparisons.OperatorIfNull;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorAConstNull;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorBIPush;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorDConst0;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorDConst1;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorFConst0;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorFConst1;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorFConst2;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorIConst0;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorIConst1;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorIConst2;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorIConst3;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorIConst4;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorIConst5;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorIConstM1;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorLConst0;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorLConst1;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorLDC;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorNop;
import tokyo.peya.langjal.vm.engine.stacking.instructions.constants.OperatorSIPush;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorAALoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorALoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorBALoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorCALoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorDALoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorDLoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorFALoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorFLoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorIALoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorILoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorLALoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorLLoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.loads.OperatorSALoad;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorDAdd;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorDDiv;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorDMul;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorDNeg;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorDRem;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorDSub;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorFAdd;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorFDiv;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorFMul;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorFNeg;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorFRem;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorFSub;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIAdd;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIAnd;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIDiv;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIInc;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIMul;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorINeg;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIOr;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIRem;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIShl;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIShr;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorISub;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIUShr;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorIXor;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLAdd;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLAnd;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLDiv;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLMul;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLNeg;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLOr;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLRem;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLShl;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLShr;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLSub;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLUShr;
import tokyo.peya.langjal.vm.engine.stacking.instructions.math.OperatorLXor;
import tokyo.peya.langjal.vm.engine.stacking.instructions.references.OperatorGetStatic;
import tokyo.peya.langjal.vm.engine.stacking.instructions.references.OperatorInvokeDynamic;
import tokyo.peya.langjal.vm.engine.stacking.instructions.references.OperatorInvokeStatic;
import tokyo.peya.langjal.vm.engine.stacking.instructions.references.OperatorInvokeVirtual;
import tokyo.peya.langjal.vm.engine.stacking.instructions.references.OperatorNew;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stack.OperatorDup;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stack.OperatorDup2;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stack.OperatorDup2X1;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stack.OperatorDup2X2;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stack.OperatorDupX1;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stack.OperatorDupX2;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stack.OperatorPop;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stack.OperatorPop2;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stack.OperatorSwap;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorAAStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorAStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorBAStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorCAStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorDAStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorDStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorFAStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorFStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorIAStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorIStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorLAStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorLStore;
import tokyo.peya.langjal.vm.engine.stacking.instructions.stores.OperatorSAStore;
import tokyo.peya.langjal.vm.exceptions.UnknownInstructionPanic;

public class VMStackMachine
{
    private static final AbstractInstructionOperator<?>[] OPERATORS = new AbstractInstructionOperator<?>[]{
            // <editor-fold desc="Constants">
            new OperatorNop(), // 0x00 - 0
            new OperatorAConstNull(), // 0x01 - 1
            new OperatorIConstM1(), // 0x02 - 2
            new OperatorIConst0(), // 0x03 - 3
            new OperatorIConst1(), // 0x04 - 4
            new OperatorIConst2(), // 0x05 - 5
            new OperatorIConst3(), // 0x06 - 6
            new OperatorIConst4(), // 0x07 - 7
            new OperatorIConst5(), // 0x08 - 8
            new OperatorLConst0(), // 0x09 - 9
            new OperatorLConst1(), // 0x0A - 10
            new OperatorFConst0(), // 0x0B - 11
            new OperatorFConst1(), // 0x0C - 12
            new OperatorFConst2(), // 0x0D - 13
            new OperatorDConst0(), // 0x0E - 14
            new OperatorDConst1(), // 0x0F - 15
            new OperatorBIPush(), // 0x10 - 16
            new OperatorSIPush(), // 0x11 - 17
            new OperatorLDC(), // 0x12 - 18
            // </editor-fold>

            // <editor-fold desc="Loads">
            new OperatorILoad(), // 0x15 - 21
            new OperatorLLoad(), // 0x16 - 22
            new OperatorFLoad(), // 0x17 - 23
            new OperatorDLoad(), // 0x18 - 24
            new OperatorALoad(), // 0x19 - 25
            new OperatorIALoad(), // 0x2E - 46
            new OperatorLALoad(), // 0x2F - 47
            new OperatorFALoad(), // 0x30 - 48
            new OperatorDALoad(), // 0x31 - 49
            new OperatorAALoad(), // 0x32 - 50
            new OperatorBALoad(), // 0x33 - 51
            new OperatorCALoad(), // 0x34 - 52
            new OperatorSALoad(), // 0x35 - 53
            // </editor-fold>

            // <editor-fold desc="Stores">
            new OperatorIStore(), // 0x36 - 54
            new OperatorLStore(), // 0x37 - 55
            new OperatorFStore(), // 0x38 - 56
            new OperatorDStore(), // 0x39 - 57
            new OperatorAStore(), // 0x3A - 58
            new OperatorIAStore(), // 0x4F - 79
            new OperatorLAStore(), // 0x50 - 80
            new OperatorFAStore(), // 0x51 - 81
            new OperatorDAStore(), // 0x52 - 82
            new OperatorAAStore(), // 0x53 - 83
            new OperatorBAStore(), // 0x54 - 84
            new OperatorCAStore(), // 0x55 - 85
            new OperatorSAStore(), // 0x56 - 86
            // </editor-fold>

            // <editor-fold desc="Stack">
            new OperatorPop(), // 0x57 - 87
            new OperatorPop2(), // 0x58 - 88
            new OperatorDup(), // 0x59 - 89
            new OperatorDupX1(), // 0x5A - 90
            new OperatorDupX2(), // 0x5B - 91
            new OperatorDup2(), // 0x5C - 92
            new OperatorDup2X1(), // 0x5D - 93
            new OperatorDup2X2(), // 0x5E - 94
            new OperatorSwap(), // 0x5F - 95
            // </editor-fold>

            // <editor-fold desc="Math Operations">
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

            // <editor-fold desc="Comparisons">
            new OperatorIfNull(),  // 0xC6 - 198
            new OperatorIfNonNull(), // 0xC7 - 199
            // </editor-fold>

            // <editor-fold desc="References">
            new OperatorGetStatic(), // 0xB2 - 178
            new OperatorInvokeVirtual(), // 0xB6 - 182
            new OperatorInvokeStatic(), // 0xB8 - 184
            new OperatorInvokeDynamic(), // 0xBA - 186

            new OperatorNew(), // 0xBB - 187
            // </editor-fold>

            // </editor-fold>
    };

    public static AbstractInstructionOperator<?> getOperator(final int opcode)
    {
        for (AbstractInstructionOperator<?> operator : OPERATORS)
            if (operator.getOpcode() == opcode)
                return operator;

        throw new UnknownInstructionPanic("No operator found for opcode: " + opcode);
    }

    @SuppressWarnings("unchecked")
    public static void executeInstruction(@NotNull VMFrame frame, @NotNull AbstractInsnNode insn)
    {
        AbstractInstructionOperator<AbstractInsnNode> operator = (AbstractInstructionOperator<AbstractInsnNode>) getOperator(
                insn.getOpcode());

        operator.execute(frame, insn);
    }
}
