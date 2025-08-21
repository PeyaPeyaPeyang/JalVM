package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.lang.reflect.Array;

@Getter
public class VMArray extends VMObject implements VMValue, VMReferenceValue
{
    private final VMType<?> elementType;
    private final VMValue[] elements;

    private final VMType<?> arrayType;

    public VMArray(@NotNull VMSystemClassLoader classLoader, @NotNull VMType<?> elementType, int size)
    {
        super(classLoader.findClass(ClassReference.of("java/util/Collection")));

        if (size < 0)
            throw new VMPanic("Size cannot be negative: " + size);
        else if (elementType.getArrayDimensions() > 0)
            throw new VMPanic("Cannot create an array of arrays: " + elementType.getTypeDescriptor());

        this.elementType = elementType;
        this.elements = new VMValue[size];
        this.fillDefaults();

        this.arrayType = VMType.of("[" + elementType.getTypeDescriptor());

        this.forceInitialise();
    }

    public VMArray(@NotNull VMClass objectType, VMType<?> elementType, VMValue[] elements, @NotNull VMType<?> arrayType)
    {
        super(objectType);
        this.elementType = elementType;
        this.elements = elements;
        this.arrayType = arrayType;

        this.forceInitialise();
    }

    private void fillDefaults()
    {
        for (int i = 0; i < this.elements.length; i++)
            this.elements[i] = this.elementType.defaultValue();
    }

    public void linkClass(@NotNull VMSystemClassLoader cl)
    {
        if (this.elementType.getArrayDimensions() > 0)
            throw new VMPanic("Cannot link an array of arrays: " + this.elementType.getTypeDescriptor());

        // 配列の型をリンクする
        this.arrayType.linkClass(cl);

        // 要素の型をリンクする
        this.elementType.linkClass(cl);
    }

    @Override
    public @NotNull VMObject cloneValue()
    {
        VMValue[] clonedElements = new VMValue[this.elements.length];
        for (int i = 0; i < this.elements.length; i++)
        {
            VMValue value = this.elements[i];
            if (value != null)
                clonedElements[i] = value.cloneValue();
            else
                clonedElements[i] = null; // nullはそのままコピー
        }

        return new VMArray(this.getObjectType(), this.elementType, clonedElements, this.arrayType);

    }

    public VMArray(@NotNull VMSystemClassLoader classLoader, @NotNull VMType<?> objectType, @NotNull VMValue[] values)
    {
        super(classLoader.findClass(ClassReference.of("java/util/Collection")));

        // 値チェック
        for (VMValue value : values)
            if (!objectType.isAssignableFrom(value.type()))
                throw new VMPanic("VM BUG!!! Incompatible type in array: " + value.type()
                                                                                  .getTypeDescriptor() + " for " + objectType.getTypeDescriptor());

        this.elementType = objectType;
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
            return new VMNull<>(this.elementType);

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
            return this.elementType.isAssignableFrom(otherArray.getObjectType());
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
    public @NotNull String toString()
    {
        StringBuilder sb = new StringBuilder("[" + this.elementType.getTypeDescriptor() + "{");
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

    public <T> T[] toArray(@NotNull Class<? extends T> type)
    {
        if (type.isPrimitive())
            throw new VMPanic("Cannot convert VMArray to primitive array: " + type.getName());

        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(type, this.elements.length);
        for (int i = 0; i < this.elements.length; i++)
        {
            VMValue value = this.elements[i];
            if (value != null)
                array[i] = type.cast(value);
            else
                array[i] = null; // nullはそのままコピー
        }
        return array;
    }

    public byte[] toByteArray()
    {
        if (this.elementType != VMType.BYTE)
            throw new VMPanic("Cannot convert VMArray to byte array: " + this.elementType.getTypeDescriptor());

        byte[] array = new byte[this.elements.length];
        for (int i = 0; i < this.elements.length; i++)
        {
            VMValue value = this.elements[i];
            if (value instanceof VMInteger integer)
                array[i] = integer.asNumber().byteValue();
            else if (value == null)
                array[i] = 0x00;
            else
                throw new VMPanic("Expected byte value at index " + i + ", but got: " + value);
        }
        return array;
    }
}
