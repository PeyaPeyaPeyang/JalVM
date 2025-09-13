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
    public int identityHashCode()
    {
        return 0;  // null は常に同じ。
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
    public VMValue conformValue(@NotNull VMType<?> expectedType)
    {
        if (expectedType.isPrimitive())
            throw new IllegalArgumentException("Cannot conform a null value to a primitive type: " + expectedType.getTypeDescriptor());

        // プリミティブ以外は参照型 -> null は常に代入可能。
        return expectedType == this.type ? this : new VMNull<>(expectedType);
    }

    @Override
    public @NotNull String toString()
    {
        return "NULL";
    }
}
