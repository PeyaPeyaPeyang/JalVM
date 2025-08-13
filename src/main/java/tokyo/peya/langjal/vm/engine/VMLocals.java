package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.NoReferencePanic;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.HashMap;
import java.util.Map;

public class VMLocals {
    private final int maxSize;
    private final Map<Integer, VMValue> locals;

    public VMLocals(int maxSize, @NotNull VMValue[] args) {
        this.maxSize = maxSize;
        this.locals = new HashMap<>();

        this.initialiseArgs(args);
    }

    public void initialiseArgs(@NotNull VMValue[] args) {
        int slot = 0;
        for (VMValue arg : args) {
            this.setSlot(slot++, arg);
            if (arg.isCategory2())
                slot++; // 次のスロットは TOP になるのでスキップ
        }
    }

    public void setSlot(int index, @NotNull VMValue value) {
        if (index < 0 || (this.maxSize > 0 && index >= this.maxSize))
            throw new NoReferencePanic("Local variable index " + index + " is out of bounds. Max size: " + this.maxSize);

        this.locals.put(index, value);
    }

    public @NotNull VMValue getLocal(int index) {
        if (index < 0 || (this.maxSize > 0 && index >= this.maxSize))
            throw new NoReferencePanic("Local variable index " + index + " is out of bounds. Max size: " + this.maxSize);

        VMValue value = this.locals.get(index);
        if (value == null)
            throw new NoReferencePanic("Local variable at index " + index + " does not exist."
                    + "May be referencing a 2-category value with its last index?");

        return value;
    }


    @Override
    public String toString() {
        return "[" + locals.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue().toString())
                .reduce((a, b) -> a + ", " + b)
                .orElse("") + "]";
    }

    public <T extends VMValue> T getType(int i, Class<T> vmIntegerClass) {
        VMValue value = this.getLocal(i);
        if (vmIntegerClass.isInstance(value)) {
            return vmIntegerClass.cast(value);
        } else {
            throw new VMPanic("Local variable at index " + i + " is not of type " + vmIntegerClass.getSimpleName());
        }
    }
}
