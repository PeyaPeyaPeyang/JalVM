package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

@Getter
public class VMArray implements VMValue, VMReferenceValue
{

    private final VMType<?> objectType;
    private final VMValue[] elements;

    private final VMType<?> arrayType;

    public VMArray(@NotNull VMType<?> objectType, int size)
    {
        if (size < 0)
            throw new VMPanic("Size cannot be negative: " + size);
        else if (objectType.getArrayDimensions() > 0)
            throw new VMPanic("Cannot create an array of arrays: " + objectType.getTypeDescriptor());

        this.objectType = objectType;
        this.elements = new VMValue[size];

        this.arrayType = VMType.of("[" + objectType.getTypeDescriptor());
    }

    public void linkClass(@NotNull VMSystemClassLoader cl)
    {
        if (this.objectType.getArrayDimensions() > 0)
            throw new VMPanic("Cannot link an array of arrays: " + this.objectType.getTypeDescriptor());

        // 配列の型をリンクする
        this.arrayType.linkClass(cl);

        // 要素の型をリンクする
        this.objectType.linkClass(cl);
    }

    public VMArray(@NotNull VMType<?> objectType, @NotNull VMValue[] values)
    {
        // 値チェック
        for (VMValue value : values)
            if (!objectType.isAssignableFrom(value.type()))
                throw new VMPanic("VM BUG!!! Incompatible type in array: " + value.type()
                                                                                  .getTypeDescriptor() + " for " + objectType.getTypeDescriptor());

        this.objectType = objectType;
        this.elements = values;

        this.arrayType = VMType.of("[" + objectType.getTypeDescriptor());
    }

    @NotNull
    public VMValue get(int index)
    {
        if (index < 0 || index >= this.elements.length)
            throw new VMPanic("Index: " + index + ", Size: " + this.elements.length);

        VMValue value = this.elements[index];
        if (value == null)
            return new VMNull<>(this.objectType);

        return value;
    }

    public void set(int index, @NotNull VMValue value)
    {
        if (index < 0 || index >= this.elements.length)
            throw new VMPanic("Index: " + index + ", Size: " + this.elements.length);
        this.elements[index] = value;
    }

    public int length()
    {
        return this.elements.length;
    }

    @Override
    public @NotNull VMType<?> type()
    {
        return this.arrayType;
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        if (other instanceof VMArray otherArray)
            return this.objectType.isAssignableFrom(otherArray.getObjectType());
        return other.type().equals(VMType.GENERIC_OBJECT) || other instanceof VMNull;
    }

    @Override
    public VMValue conformValue(@NotNull VMType<?> expectedType)
    {
        if (expectedType.isAssignableFrom(this.arrayType))
            return this;

        throw new VMPanic("Cannot conform an array of type: " + this.arrayType.getTypeDescriptor() +
                          " to the expected type: " + expectedType.getTypeDescriptor());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("[" + this.objectType.getTypeDescriptor() + "{");
        for (int i = 0; i < this.elements.length; i++)
        {
            if (i > 0) sb.append(", ");
            VMValue value = this.elements[i];
            if (value == null)
                sb.append("?");
            else
                sb.append(value);
        }
        sb.append("}");
        return sb.toString();
    }
}
