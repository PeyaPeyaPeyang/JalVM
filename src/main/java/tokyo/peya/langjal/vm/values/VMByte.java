package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMByte extends VMInteger {
    public static final VMByte ZERO = new VMByte((byte) 0x00);

    private VMByte(final byte value) {
        super(PrimitiveTypes.BYTE, value);
    }

    @Override
    public String toString() {
        return String.format("0x%02X", this.asNumber().byteValue());
    }
}
