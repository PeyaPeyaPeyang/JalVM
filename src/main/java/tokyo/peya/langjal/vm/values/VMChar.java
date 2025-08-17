package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public final class VMChar extends VMInteger
{
    public static final VMChar ZERO = new VMChar((char) 0);

    public VMChar(final char value)
    {
        super(VMType.CHAR, value);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        if (other instanceof VMChar)
            return true;

        if (other instanceof VMInteger intVal)
        {
            int value = intVal.asNumber().intValue();
            return value >= Character.MIN_VALUE && value <= Character.MAX_VALUE;
        }

        return false;
    }

    @Override
    public String toString()
    {
        return "'" + (char) this.asNumber().intValue() + "'";
    }
}
