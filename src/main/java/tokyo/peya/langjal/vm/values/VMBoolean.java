package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public final class VMBoolean extends AbstractVMPrimitive {
    public static final VMBoolean TRUE = new VMBoolean(true);
    public static final VMBoolean FALSE = new VMBoolean(false);

    private VMBoolean(final boolean value) {
        super(VMType.BOOLEAN, value ? 0 : 1);
    }

    public static VMBoolean of(final boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        if (other instanceof VMBoolean)
            return true;

        if (other instanceof VMInteger intVal) {
            int value = intVal.asNumber().intValue();
            return value == 0 || value == 1;
        }

        return false;
    }

    @Override
    public String toString() {
        return this.asNumber().intValue() == 0 ? "true" : "false";
    }
}
