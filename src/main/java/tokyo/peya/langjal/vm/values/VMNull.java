package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public record VMNull(VMType type) implements VMValue, VMReferenceValue
{
    public VMNull(@NotNull VMType type)
    {
        this.type = type;
    }

    @Override
    public @NotNull VMType type()
    {
        return this.type;
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMNull || this.type.equals(other.type());
    }

    @Override
    public String toString()
    {
        return "NULL";
    }
}
