package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.HashMap;
import java.util.Map;

public class VMLocals {
    private final int maxSize;
    private final Map<Integer, VMValue> locals;

    public VMLocals(int maxSize) {
        this.maxSize = maxSize;
        this.locals = new HashMap<>();
    }

    public void setLocal(int index, @NotNull VMValue value) {
        this.locals.put(index, value);
    }

    public @NotNull VMValue getLocal(int index) {
        VMValue value = this.locals.get(index);
        if (value == null)
            throw new IllegalArgumentException("Local variable at index " + index + " does not exist."
                    + "May be referencing a 2-category value with its last index?");

        return value;
    }
}
