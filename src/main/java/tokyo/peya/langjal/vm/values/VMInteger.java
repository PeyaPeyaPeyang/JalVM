package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;

public sealed class VMInteger extends AbstractVMPrimitive permits VMByte, VMChar, VMShort
{
    protected final JalVM vm;

    protected VMInteger(@NotNull VMComponent component, @NotNull VMType<?> type, final int value)
    {
        super(type, value);
        this.vm = component.getVM();
    }

    public VMInteger(@NotNull VMComponent component, final int value)
    {
        this(component, VMType.of(component, PrimitiveTypes.INT), value);
    }

    public static VMInteger ofZero(@NotNull VMComponent component)
    {
        return new VMInteger(component, 0);
    }

    @NotNull
    public VMInteger add(@NotNull VMInteger other)
    {
        return new VMInteger(this.vm, this.asNumber().intValue() + other.asNumber().intValue());
    }

    @Override
    public int identityHashCode()
    {
        return Integer.hashCode(this.asNumber().intValue());
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMInteger;
    }

    @Override
    public @NotNull VMInteger cloneValue()
    {
        return new VMInteger(this.vm, this.asNumber().intValue());
    }

    @Override
    public @NotNull String toString()
    {
        return String.valueOf(this.asNumber().intValue());
    }

    public @NotNull VMInteger sub(VMInteger val1)
    {
        return new VMInteger(this.vm, this.asNumber().intValue() - val1.asNumber().intValue());
    }

    public @NotNull VMInteger mul(VMInteger val1)
    {
        return new VMInteger(this.vm, this.asNumber().intValue() * val1.asNumber().intValue());
    }

    public @NotNull VMInteger div(VMInteger val1)
    {
        if (val1.asNumber().intValue() == 0)
            throw new IllegalOperandPanic("Division by zero");
        return new VMInteger(this.vm, this.asNumber().intValue() / val1.asNumber().intValue());
    }

    public @NotNull VMInteger rem(VMInteger val1)
    {
        return new VMInteger(this.vm, this.asNumber().intValue() % val1.asNumber().intValue());
    }

    public @NotNull VMInteger neg()
    {
        return new VMInteger(this.vm, -this.asNumber().intValue());
    }

    public @NotNull VMInteger shl(VMInteger val1)
    {
        return new VMInteger(this.vm, this.asNumber().intValue() << val1.asNumber().intValue());
    }

    public @NotNull VMInteger shr(VMInteger val1)
    {
        return new VMInteger(this.vm, this.asNumber().intValue() >> val1.asNumber().intValue());
    }

    public @NotNull VMInteger ushr(VMInteger val1)
    {
        return new VMInteger(this.vm, this.asNumber().intValue() >>> val1.asNumber().intValue());
    }

    public @NotNull VMInteger and(VMInteger val1)
    {
        return new VMInteger(this.vm, this.asNumber().intValue() & val1.asNumber().intValue());
    }

    public @NotNull VMInteger or(VMInteger val1)
    {
        return new VMInteger(this.vm, this.asNumber().intValue() | val1.asNumber().intValue());
    }

    public @NotNull VMInteger xor(VMInteger val1)
    {
        return new VMInteger(this.vm, this.asNumber().intValue() ^ val1.asNumber().intValue());
    }

    @Override
    public VMValue conformValue(@NotNull VMType<?> expectedType)
    {
        if (this.type().equals(expectedType))
            return this;

        if (expectedType.getType() ==  PrimitiveTypes.BYTE)
        {
            int value = this.asNumber().intValue();
            if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE)
                throw new IllegalOperandPanic("Value out of range for byte: " + value);

            return new VMByte(this.vm, (byte) value);
        }
        else if (expectedType.getType() == PrimitiveTypes.CHAR)
        {
            int value = this.asNumber().intValue();
            if (value < Character.MIN_VALUE || value > Character.MAX_VALUE)
                throw new IllegalOperandPanic("Value out of range for char: " + value);

            return new VMChar(this.vm, (char) value);
        }
        else if (expectedType.getType() == PrimitiveTypes.SHORT)
        {
            int value = this.asNumber().intValue();
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE)
                throw new IllegalOperandPanic("Value out of range for short: " + value);

            return new VMShort(this.vm, (short) value);
        }
        else if (expectedType.getType() == PrimitiveTypes.BOOLEAN)
        {
            int value = this.asNumber().intValue();
            if (value < 0 || value > 1)
                throw new IllegalOperandPanic("Value out of range for boolean: " + value);

            return VMBoolean.of(this.vm, value == 1);
        }
        else if (expectedType.getType() == PrimitiveTypes.LONG)
            return new VMLong(this.vm, this.asNumber().longValue());  // 長整数に変換
        else if (expectedType.getType() == PrimitiveTypes.DOUBLE)
            return new VMDouble(this.vm, this.asNumber().doubleValue());  // 浮動小数点数に変換
        else if (expectedType.getType() == PrimitiveTypes.FLOAT)
            return new VMFloat(this.vm, this.asNumber().floatValue());  // 浮動小数点数に変換
        else if (expectedType.getType() == PrimitiveTypes.INT)
            return new VMInteger(this.vm, this.asNumber().intValue());  // そのまま返す

        return super.conformValue(expectedType);
    }
}
