package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMShort extends AbstractVMPrimitive {
    public static final VMShort ZERO = new VMShort((short) 0);

    public VMShort(final short value) {
        super(PrimitiveTypes.SHORT, value);
    }
}
