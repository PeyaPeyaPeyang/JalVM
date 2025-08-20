package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;

public interface VMValue
{
    @NotNull
    VMType<?> type();

    boolean isCompatibleTo(@NotNull VMValue other);

    default boolean isCategory2()
    {
        return this.type().getType().getCategory() == 2;
    }

    String toString();

    default Object toJavaObject()
    {
        if (this instanceof VMPrimitive)
            return ((VMPrimitive) this).asNumber();
        else
            throw new VMPanic("Cannot convert " + this.getClass().getName());
    }

    static VMValue fromJavaObject(@NotNull VMSystemClassLoader cl, @NotNull Object value)
    {
        VMValue vmValue = switch (value)
        {
            case Integer intValue -> new VMInteger(intValue);
            case Long longValue -> new VMLong(longValue);
            case Float floatValue -> new VMFloat(floatValue);
            case String strValue -> VMStringCreator.createString(cl, strValue);
            case Double doubleValue -> new VMDouble(doubleValue);
            case Character charValue -> new VMChar(charValue);
            case Byte byteValue -> new VMByte(byteValue);
            case Short shortValue -> new VMShort(shortValue);
            case Boolean boolValue -> VMBoolean.of(boolValue);
            case Type asmType  -> switch (asmType.getSort())
            {
                case Type.VOID: new VMClassObject(cl, VMType.VOID);
                case Type.BOOLEAN: new VMClassObject(cl, VMType.BOOLEAN);
                case Type.BYTE: new VMClassObject(cl, VMType.BYTE);
                case Type.CHAR: new VMClassObject(cl, VMType.CHAR);
                case Type.SHORT: new VMClassObject(cl, VMType.SHORT);
                case Type.INT: new VMClassObject(cl, VMType.INTEGER);
                case Type.FLOAT: new VMClassObject(cl, VMType.FLOAT);
                case Type.LONG: new VMClassObject(cl, VMType.LONG);
                case Type.DOUBLE: new VMClassObject(cl, VMType.DOUBLE);
                case Type.ARRAY, Type.OBJECT: {
                    VMType<?> vmType = VMType.of(TypeDescriptor.parse(asmType.getDescriptor()));
                    yield new VMClassObject(cl, vmType);
                }

                default:
                    throw new IllegalStateException("Unexpected value: " + asmType.getSort());
            };
            default -> throw new VMPanic("Unsupported constant type: " + value.getClass().getName());
        };
        vmValue.type().linkClass(cl);
        return vmValue;
    }

    default VMValue conformValue(@NotNull VMType<?> expectedType)
    {
        // 値を暗黙的に変換する場合は，型が一致しているかどうかを確認する
        if (this.type().equals(expectedType))
            return this;

        throw new VMPanic("Cannot conform " + this.type() + " to " + expectedType);
    }
}
