package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMLong extends AbstractVMPrimitive {
    public static final VMLong ZERO = new VMLong(0);

    public VMLong(final long value) {
        super(PrimitiveTypes.LONG, value);
    }

    @Override
    public String toString() {
        return String.valueOf(this.asNumber().longValue());
    }
}
