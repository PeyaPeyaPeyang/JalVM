package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMShort extends VMInteger {
    public static final VMShort ZERO = new VMShort((short) 0);

    public VMShort(final short value) {
        super(PrimitiveTypes.SHORT, value);
    }
    @Override
    public String toString() {
        return String.valueOf(this.asNumber().shortValue()) + "s";
    }
}
