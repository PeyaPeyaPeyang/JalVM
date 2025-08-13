package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMByte extends VMInteger {
    public static final VMByte ZERO = new VMByte((byte) 0x00);

    private VMByte(final byte value) {
        super(VMType.BYTE, value);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        if (other instanceof VMByte)
            return true;

        if (other instanceof VMInteger intVal) {
            int value = intVal.asNumber().intValue();
            return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
        }

        return false;
    }

    @Override
    public String toString() {
        return String.format("0x%02X", this.asNumber().byteValue());
    }
}
