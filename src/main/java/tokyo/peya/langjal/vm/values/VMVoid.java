package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public class VMVoid implements VMValue
{
    public static final VMVoid INSTANCE = new VMVoid();

    private VMVoid()
    {
    }

    @Override
    public @NotNull VMType type()
    {
        return VMType.VOID;
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMVoid;
    }

    @Override
    public String toString()
    {
        return "VOID";
    }
}
