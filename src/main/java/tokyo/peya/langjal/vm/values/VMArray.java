package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

@Getter
public class VMArray implements VMValue {

    private final VMType objectType;
    private final VMValue[] elements;

    private final VMType arrayType;

    public VMArray(@NotNull VMType objectType, int size) {
        if (size <= 0)
            throw new VMPanic("Size cannot be zero or negative: " + size);
        else if (objectType.getArrayDimensions() > 0)
            throw new VMPanic("Cannot create an array of arrays: " + objectType.getTypeDescriptor());

        this.objectType = objectType;
        this.elements = new VMValue[size];

        this.arrayType = VMType.ofTypeDescriptor("[" + objectType.getTypeDescriptor());
    }

    @Nullable
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
}
