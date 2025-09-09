package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMArrayClass;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.panics.VMPanic;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

@Getter
public class VMArray implements VMValue, VMReferenceValue
{
    private final JalVM vm;
    private final VMType<?> elementType;
    private final VMValue[] elements;

    private final long identity;

    private final VMType<?> arrayType;

    private VMObject superObject; // java/lang/Object としての振る舞いを提供するため

    public VMArray(@NotNull VMComponent component, @NotNull VMType<?> elementType, int size)
    {
        this.vm = component.getVM();
        if (size < 0)
            throw new VMPanic("Size cannot be negative: " + size);

        this.elementType = elementType;
        this.elements = new VMValue[size];
        this.fillDefaults();
        this.identity = new Random().nextLong();

        this.arrayType = VMType.of(component, "[" + elementType.getTypeDescriptor());
    }

    public VMArray(@NotNull VMComponent component, @NotNull VMType<?> elementType)
    {
        this(component, elementType, 0);
    }

    public VMArray(@NotNull VMComponent component, VMType<?> elementType, VMValue[] elements, @NotNull VMType<?> arrayType)
    {
        this.vm = component.getVM();
        this.elementType = elementType;
        this.elements = elements;
        this.arrayType = arrayType;
        this.identity = new Random().nextLong();
    }

    public VMArray(@NotNull VMComponent component, @NotNull VMType<?> elementType, @NotNull VMValue[] values)
    {
        this.vm = component.getVM();

        // 値チェック
        for (VMValue value : values)
            if (!elementType.isAssignableFrom(value.type()))
                throw new VMPanic("VM BUG!!! Incompatible type in array: " + value.type()
                                                                                  .getTypeDescriptor() + " for " + elementType.getTypeDescriptor());

        this.elementType = elementType;
        this.elements = Arrays.copyOf(values, values.length);
        this.identity = new Random().nextLong();

        this.arrayType = VMType.of(component, "[" + elementType.getTypeDescriptor());
    }

    @NotNull
    public VMObject getSuperObject()
    {
        if (this.superObject == null)
            this.superObject = new PseudoSuperObject(this.vm);
        return this.superObject;
    }

    private void fillDefaults()
    {
        for (int i = 0; i < this.elements.length; i++)
            this.elements[i] = this.elementType.defaultValue();
    }

    @Override
    public @NotNull VMArray cloneValue()
    {
        VMValue[] clonedElements = new VMValue[this.elements.length];
        System.arraycopy(this.elements, 0, clonedElements, 0, this.elements.length);

        return new VMArray(this.elementType, this.elementType, clonedElements, this.arrayType);
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
    public int identityHashCode()
    {
        return 0;
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        if (other instanceof VMArray otherArray)
            return this.elementType.isAssignableFrom(otherArray.elementType);
        return other.type().equals(VMType.ofGenericObject(other.type().getVM()))  // Object
                || other instanceof VMNull;  // null
    }

    @Override
    public boolean isCategory2()
    {
        return false;
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
        if (this.elementType != VMType.of(this.vm, PrimitiveTypes.BYTE))
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

    private class PseudoSuperObject extends VMObject
    {
        public PseudoSuperObject(@NotNull VMComponent component)
        {
            super(VMType.ofGenericObject(component).getLinkedClass());

            this.forceInitialise(component.getClassLoader());
        }

        @Override
        public VMClass getObjectType()
        {
            return VMArray.this.arrayType.getLinkedClass();
        }

        @Override
        public @NotNull VMReferenceValue cloneValue()
        {
            return VMArray.this.cloneValue();
        }
    }
}
