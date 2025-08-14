package tokyo.peya.langjal.vm.engine.stacking.instructions.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.LdcInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.*;

public class OperatorLDC extends AbstractInstructionOperator<LdcInsnNode> {
    public OperatorLDC() {
        super(EOpcodes.LDC, "ldc");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull LdcInsnNode operand) {
        VMStack stack = frame.getStack();
        Object value = operand.cst;
        VMValue toValue = switch (value) {
            case Integer intValue -> new VMInteger(intValue);
            case Long longValue -> new VMLong(longValue);
            case Float floatValue -> new VMFloat(floatValue);
            case String strValue -> VMStringCreator.createString(strValue);
            case Double doubleValue -> new VMDouble(doubleValue);
            case Character charValue -> new VMChar(charValue);
            case Byte byteValue -> new VMByte(byteValue);
            case Short shortValue -> new VMShort(shortValue);
            case Boolean boolValue -> VMBoolean.of(boolValue);

            default -> throw new VMPanic("Unsupported constant type: " + value.getClass().getName());
        };

        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(toValue, frame.getMethod(), operand)
        );
        stack.push(toValue);
    }
}
