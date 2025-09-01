package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

public interface VMValue
{
    @NotNull
    VMType<?> type();

    int identityHashCode();
    boolean isCompatibleTo(@NotNull VMValue other);

    default boolean isCategory2()
    {
        return this.type().getType().getCategory() == 2;
    }

    @NotNull
    VMValue cloneValue();

    default Object toJavaObject()
    {
        if (this instanceof VMPrimitive)
            return ((VMPrimitive) this).asNumber();
        else
            throw new VMPanic("Cannot convert " + this.getClass().getName());
    }

    static VMValue fromJavaObject(@NotNull VMComponent component, @NotNull Object value)
    {
        JalVM vm = component.getVM();
        return switch (value)
        {
            case Integer intValue -> new VMInteger(vm, intValue);
            case Long longValue -> new VMLong(vm, longValue);
            case Float floatValue -> new VMFloat(vm, floatValue);
            case String strValue -> VMStringObject.createString(vm, strValue);
            case Double doubleValue -> new VMDouble(vm, doubleValue);
            case Character charValue -> new VMChar(vm, charValue);
            case Byte byteValue -> new VMByte(vm, byteValue);
            case Short shortValue -> new VMShort(vm, shortValue);
            case Boolean boolValue -> VMBoolean.of(vm, boolValue);
            case Type asmType  -> switch (asmType.getSort())
            {
                case Type.VOID: new VMClassObject(VMType.of(vm, PrimitiveTypes.VOID));
                case Type.BOOLEAN: new VMClassObject(VMType.of(vm, PrimitiveTypes.BOOLEAN));
                case Type.BYTE: new VMClassObject(VMType.of(vm, PrimitiveTypes.BYTE));
                case Type.CHAR: new VMClassObject(VMType.of(vm, PrimitiveTypes.CHAR));
                case Type.SHORT: new VMClassObject(VMType.of(vm, PrimitiveTypes.SHORT));
                case Type.INT: new VMClassObject(VMType.of(vm, PrimitiveTypes.INT));
                case Type.FLOAT: new VMClassObject(VMType.of(vm, PrimitiveTypes.FLOAT));
                case Type.LONG: new VMClassObject(VMType.of(vm, PrimitiveTypes.LONG));
                case Type.DOUBLE: new VMClassObject(VMType.of(vm, PrimitiveTypes.DOUBLE));
                case Type.ARRAY, Type.OBJECT: {
                    VMType<?> vmType = VMType.of(vm, TypeDescriptor.parse(asmType.getDescriptor()));
                    yield vmType.getLinkedClass().getClassObject();
                }

                default:
                    throw new IllegalStateException("Unexpected value: " + asmType.getSort());
            };
            default -> throw new VMPanic("Unsupported constant type: " + value.getClass().getName());
        };
    }

    default VMValue conformValue( @NotNull VMType<?> expectedType)
    {
        if (expectedType.equals(VMType.ofGenericObject(expectedType.getVM())) || this.type().equals(expectedType))
            return this;  // Object 型にはすべての型が適合する，また型が同じならそのまま返す

        throw new VMPanic("Cannot conform " + this.type() + " to " + expectedType);
    }
}
