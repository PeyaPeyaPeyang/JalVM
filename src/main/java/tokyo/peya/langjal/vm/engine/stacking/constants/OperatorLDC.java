package tokyo.peya.langjal.vm.engine.stacking.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.LdcInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.values.*;

public class OperatorLDC extends AbstractInstructionOperator<LdcInsnNode> {
    public OperatorLDC() {
        super(EOpcodes.LDC, "ldc");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull LdcInsnNode operand) {
        Object value = operand.cst;
        VMStack stack = frame.getStack();
        switch (value) {
            case Integer intValue -> stack.push(new VMInteger(intValue));
            case Long longValue -> stack.push(new VMLong(longValue));
            case Float floatValue -> stack.push(new VMFloat(floatValue));
            case String strValue -> stack.push(VMStringCreator.createString(strValue));
            case Double doubleValue -> stack.push(new VMDouble(doubleValue));
            case Character charValue -> stack.push(new VMChar(charValue));
            case Byte byteValue -> stack.push(new VMByte(byteValue));
            case Short shortValue -> stack.push(new VMShort(shortValue));
            case Boolean boolValue -> stack.push(VMBoolean.of(boolValue));

            default -> {
                throw new VMPanic("Unsupported constant type: " + value.getClass().getName());
            }
        }
    }
}
