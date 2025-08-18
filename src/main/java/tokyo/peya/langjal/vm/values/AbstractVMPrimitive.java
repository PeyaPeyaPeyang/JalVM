package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractVMPrimitive implements VMPrimitive
{
    private final VMType<?> type;
    private final Number rawValue;

    protected AbstractVMPrimitive(@NotNull VMType<?> type, @NotNull Number rawValue)
    {
        this.type = type;
        this.rawValue = rawValue;
    }

    @Override
    public @NotNull VMType<?> type()
    {
        return this.type;
    }

    @Override
    public @NotNull Number asNumber()
    {
        return this.rawValue;
    }
}
