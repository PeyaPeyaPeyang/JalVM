package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public final class VMShort extends VMInteger
{
    public static final VMShort ZERO = new VMShort((short) 0);

    public VMShort(final short value)
    {
        super(VMType.SHORT, value);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        if (other instanceof VMShort)
            return true;

        if (other instanceof VMInteger intVal)
        {
            int value = intVal.asNumber().intValue();
            return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
        }

        return false;
    }

    @Override
    public @NotNull VMShort cloneValue()
    {
        return new VMShort(this.asNumber().shortValue());
    }

    @Override
    public @NotNull String toString()
    {
        return String.valueOf(this.asNumber().shortValue()) + "s";
    }
}
