package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public abstract class AbstractVMPrimitive implements VMPrimitive {
    private final PrimitiveTypes type;
    private final Number rawValue;

    protected AbstractVMPrimitive(@NotNull PrimitiveTypes type, @NotNull Number rawValue) {
        this.type = type;
        this.rawValue = rawValue;
    }

    @Override
    public @NotNull PrimitiveTypes getType() {
        return this.type;
    }

    @Override
    public @NotNull Number asNumber() {
        return this.rawValue;
    }
}
