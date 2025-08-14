package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public class VMNull implements VMValue, VMReferenceValue {
    private final VMType type;

    public VMNull(@NotNull VMType type) {
        this.type = type;
    }

    @Override
    public @NotNull VMType getType() {
        return this.type;
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        return other instanceof VMNull || this.type.equals(other.getType());
    }

    @Override
    public String toString() {
        return "NULL";
    }
}
