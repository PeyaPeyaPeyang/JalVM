package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public final class VMBoolean extends AbstractVMPrimitive
{
    public static final VMBoolean TRUE = new VMBoolean(true);
    public static final VMBoolean FALSE = new VMBoolean(false);

    private VMBoolean(final boolean value)
    {
        super(VMType.BOOLEAN, value ? 1: 0);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        if (other instanceof VMBoolean)
            return true;

        if (other instanceof VMInteger intVal)
        {
            int value = intVal.asNumber().intValue();
            return value == 0 || value == 1;
        }

        return false;
    }

    @Override
    public @NotNull VMBoolean cloneValue()
    {
        return this == TRUE ? TRUE : FALSE;
    }

    @Override
    public @NotNull String toString()
    {
        return this.asNumber().intValue() == 0 ? "false": "true";
    }

    public static VMBoolean of(final boolean value)
    {
        return value ? TRUE: FALSE;
    }

    @Override
    public VMValue conformValue(@NotNull VMType<?> expectedType)
    {
        if (expectedType.equals(VMType.BOOLEAN))
            return this;
        else if (expectedType.equals(VMType.INTEGER))
            return new VMInteger(this.asNumber().intValue());

        return super.conformValue(expectedType);
    }

    public boolean asBoolean()
    {
        return this.asNumber().intValue() != 0;
    }
}
