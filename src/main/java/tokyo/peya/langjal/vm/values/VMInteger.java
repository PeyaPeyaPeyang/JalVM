package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public sealed class VMInteger extends AbstractVMPrimitive permits VMByte, VMChar, VMShort {
    public static final VMInteger ZERO = new VMInteger(0);

    protected VMInteger(PrimitiveTypes type, final int value) {
        super(type, value);
    }

    private VMInteger(final int value) {
        super(PrimitiveTypes.INT, value);
    }

    @NotNull
    public VMInteger add(@NotNull VMInteger other) {
        return new VMInteger(this.asNumber().intValue() + other.asNumber().intValue());
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        return other instanceof VMInteger;
    }

    @Override
    public String toString() {
        return String.valueOf(this.asNumber().intValue());
    }
}
