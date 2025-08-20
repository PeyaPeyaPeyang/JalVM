package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public record VMNull<T extends VMValue>(VMType<T> type) implements VMValue, VMReferenceValue
{
    public VMNull(@NotNull VMType<T> type)
    {
        this.type = type;
    }

    @Override
    public @NotNull VMType<T> type()
    {
        return this.type;
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMNull || this.type.equals(other.type());
    }

    @Override
    public @NotNull VMValue cloneValue()
    {
        return new VMNull<>(this.type);
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof VMNull<?>;  // 型は区別しない。
    }

    @Override
    public @NotNull String toString()
    {
        return "NULL";
    }
}
