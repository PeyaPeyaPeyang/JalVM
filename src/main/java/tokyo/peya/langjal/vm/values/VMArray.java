package tokyo.peya.langjal.vm.values;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

@Getter
public class VMArray {
    private final VMType objectType;
    private final VMValue[] elements;

    public VMArray(@NotNull VMType objectType, int size) {
        this.objectType = objectType;
        this.elements = new VMValue[size];
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
}
