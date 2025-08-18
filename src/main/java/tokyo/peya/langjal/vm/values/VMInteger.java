package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;

public sealed class VMInteger extends AbstractVMPrimitive permits VMByte, VMChar, VMShort
{
    public static final VMInteger ZERO = new VMInteger(0);

    protected VMInteger(VMType type, final int value)
    {
        super(type, value);
    }

    public VMInteger(final int value)
    {
        super(VMType.INTEGER, value);
    }

    @NotNull
    public VMInteger add(@NotNull VMInteger other)
    {
        return new VMInteger(this.asNumber().intValue() + other.asNumber().intValue());
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMInteger;
    }

    @Override
    public String toString()
    {
        return String.valueOf(this.asNumber().intValue());
    }

    public @NotNull VMInteger sub(VMInteger val1)
    {
        return new VMInteger(this.asNumber().intValue() - val1.asNumber().intValue());
    }

    public @NotNull VMInteger mul(VMInteger val1)
    {
        return new VMInteger(this.asNumber().intValue() * val1.asNumber().intValue());
    }

    public @NotNull VMInteger div(VMInteger val1)
    {
        if (val1.asNumber().intValue() == 0)
            throw new IllegalOperandPanic("Division by zero");
        return new VMInteger(this.asNumber().intValue() / val1.asNumber().intValue());
    }

    public @NotNull VMInteger rem(VMInteger val1)
    {
        return new VMInteger(this.asNumber().intValue() % val1.asNumber().intValue());
    }

    public @NotNull VMInteger neg()
    {
        return new VMInteger(-this.asNumber().intValue());
    }

    public @NotNull VMInteger shl(VMInteger val1)
    {
        return new VMInteger(this.asNumber().intValue() << val1.asNumber().intValue());
    }

    public @NotNull VMInteger shr(VMInteger val1)
    {
        return new VMInteger(this.asNumber().intValue() >> val1.asNumber().intValue());
    }

    public @NotNull VMInteger ushr(VMInteger val1)
    {
        return new VMInteger(this.asNumber().intValue() >>> val1.asNumber().intValue());
    }

    public @NotNull VMInteger and(VMInteger val1)
    {
        return new VMInteger(this.asNumber().intValue() & val1.asNumber().intValue());
    }

    public @NotNull VMInteger or(VMInteger val1)
    {
        return new VMInteger(this.asNumber().intValue() | val1.asNumber().intValue());
    }

    public @NotNull VMInteger xor(VMInteger val1)
    {
        return new VMInteger(this.asNumber().intValue() ^ val1.asNumber().intValue());
    }

    @Override
    public VMValue conformValue(@NotNull VMType expectedType)
    {
        if (this.type().equals(expectedType))
            return this;

        if (expectedType.equals(VMType.BYTE))
        {
            int value = this.asNumber().intValue();
            if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE)
                throw new IllegalOperandPanic("Value out of range for byte: " + value);

            return new VMByte((byte) value);
        }
        else if (expectedType.equals(VMType.CHAR))
        {
            int value = this.asNumber().intValue();
            if (value < Character.MIN_VALUE || value > Character.MAX_VALUE)
                throw new IllegalOperandPanic("Value out of range for char: " + value);

            return new VMChar((char) value);
        }
        else if (expectedType.equals(VMType.SHORT))
        {
            int value = this.asNumber().intValue();
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE)
                throw new IllegalOperandPanic("Value out of range for short: " + value);

            return new VMShort((short) value);
        }
        else if (expectedType.equals(VMType.BOOLEAN))
        {
            int value = this.asNumber().intValue();
            if (value < 0 || value > 1)
                throw new IllegalOperandPanic("Value out of range for boolean: " + value);

            return VMBoolean.of(value == 1);
        }

        return super.conformValue(expectedType);
    }
}
