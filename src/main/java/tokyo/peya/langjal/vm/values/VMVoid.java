package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.compiler.jvm.Type;

public class VMVoid implements VMValue {
    public static final VMVoid INSTANCE = new VMVoid();

    private VMVoid() {
    }

    @Override
    public @NotNull VMType getType() {
        return VMType.VOID;
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        return other instanceof VMVoid;
    }

    @Override
    public String toString() {
        return "VOID";
    }
}
