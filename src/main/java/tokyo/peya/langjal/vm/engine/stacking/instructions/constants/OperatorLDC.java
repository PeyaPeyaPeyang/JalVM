package tokyo.peya.langjal.vm.engine.stacking.instructions.constants;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.stacking.VMStack;
import tokyo.peya.langjal.vm.engine.stacking.instructions.AbstractInstructionOperator;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.tracing.ValueTracingEntry;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMByte;
import tokyo.peya.langjal.vm.values.VMChar;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.VMDouble;
import tokyo.peya.langjal.vm.values.VMFloat;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMShort;
import tokyo.peya.langjal.vm.values.VMStringCreator;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class OperatorLDC extends AbstractInstructionOperator<LdcInsnNode>
{
    public OperatorLDC()
    {
        super(EOpcodes.LDC, "ldc");
    }

    @Override
    public void execute(@NotNull VMFrame frame, @NotNull LdcInsnNode operand)
    {
        VMStack stack = frame.getStack();
        Object value = operand.cst;
        VMValue toValue = switch (value)
        {
            case Integer intValue -> new VMInteger(intValue);
            case Long longValue -> new VMLong(longValue);
            case Float floatValue -> new VMFloat(floatValue);
            case String strValue -> VMStringCreator.createString(strValue);
            case Double doubleValue -> new VMDouble(doubleValue);
            case Character charValue -> new VMChar(charValue);
            case Byte byteValue -> new VMByte(byteValue);
            case Short shortValue -> new VMShort(shortValue);
            case Boolean boolValue -> VMBoolean.of(boolValue);
            case Type asmType  -> switch (asmType.getSort())
            {
                case Type.VOID: new VMClassObject(frame.getVm(), VMType.VOID);
                case Type.BOOLEAN: new VMClassObject(frame.getVm(), VMType.BOOLEAN);
                case Type.BYTE: new VMClassObject(frame.getVm(), VMType.BYTE);
                case Type.CHAR: new VMClassObject(frame.getVm(), VMType.CHAR);
                case Type.SHORT: new VMClassObject(frame.getVm(), VMType.SHORT);
                case Type.INT: new VMClassObject(frame.getVm(), VMType.INTEGER);
                case Type.FLOAT: new VMClassObject(frame.getVm(), VMType.FLOAT);
                case Type.LONG: new VMClassObject(frame.getVm(), VMType.LONG);
                case Type.DOUBLE: new VMClassObject(frame.getVm(), VMType.DOUBLE);
                case Type.ARRAY, Type.OBJECT: {
                    VMType vmType = new VMType(TypeDescriptor.parse(asmType.getDescriptor()));
                    yield new VMClassObject(frame.getVm(), vmType);
                }

                default:
                    throw new IllegalStateException("Unexpected value: " + asmType.getSort());
            };

            default -> throw new VMPanic("Unsupported constant type: " + value.getClass().getName());
        };

        frame.getTracer().pushHistory(
                ValueTracingEntry.generation(toValue, frame.getMethod(), operand)
        );
        stack.push(toValue);
    }
}
