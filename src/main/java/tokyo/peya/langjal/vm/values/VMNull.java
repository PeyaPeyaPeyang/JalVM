package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.Type;

public class VMNull implements VMValue {
    private final Type type;

    public VMNull(@NotNull Type type) {
        this.type = type;
    }

    @Override
    public @NotNull Type getType() {
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
