package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

@Getter
public class VMArray implements VMValue, VMReferenceValue {

    private final VMType objectType;
    private final VMValue[] elements;

    private final VMType arrayType;

    public VMArray(@NotNull VMType objectType, int size) {
        if (size < 0)
            throw new VMPanic("Size cannot be negative: " + size);
        else if (objectType.getArrayDimensions() > 0)
            throw new VMPanic("Cannot create an array of arrays: " + objectType.getTypeDescriptor());

        this.objectType = objectType;
        this.elements = new VMValue[size];

        this.arrayType = VMType.ofTypeDescriptor("[" + objectType.getTypeDescriptor());
    }

    public VMArray(@NotNull VMType objectType, @NotNull VMValue[] values) {
        if (values.length == 0)
            throw new VMPanic("Array cannot be empty");

        // 値チェック
        for (VMValue value : values)
            if (!objectType.isAssignableFrom(value.getType()))
                throw new VMPanic("VM BUG!!! Incompatible type in array: " + value.getType().getTypeDescriptor() + " for " + objectType.getTypeDescriptor());

        this.objectType = objectType;
        this.elements = values;

        this.arrayType = VMType.ofTypeDescriptor("[" + objectType.getTypeDescriptor());
    }


    @NotNull
    public VMValue get(int index) {
        if (index < 0 || index >= this.elements.length)
            throw new VMPanic("Index: " + index + ", Size: " + this.elements.length);

        VMValue value = this.elements[index];
        if (value == null)
            return new VMNull(this.objectType);

        return value;
    }

    public void set(int index, @NotNull VMValue value) {
        if (index < 0 || index >= this.elements.length)
            throw new VMPanic("Index: " + index + ", Size: " + this.elements.length);
        this.elements[index] = value;
    }

    public int length() {
        return this.elements.length;
    }

    @Override
    public @NotNull VMType getType() {
        return this.arrayType;
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        if (other instanceof VMArray otherArray)
            return this.objectType.isAssignableFrom(otherArray.getObjectType());
        return other instanceof VMNull;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < this.elements.length; i++) {
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
