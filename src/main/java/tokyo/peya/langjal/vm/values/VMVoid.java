package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

public class VMVoid implements VMPrimitive
{
    public static final VMVoid INSTANCE = new VMVoid();

    private VMVoid()
    {
    }

    @Override
    public @NotNull VMType<?> type()
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

    @Override
    public @NotNull Number asNumber()
    {
        throw new VMPanic("Cannot convert VMVoid to Number");
    }
}
