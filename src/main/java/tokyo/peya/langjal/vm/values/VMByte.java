package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMByte extends AbstractVMPrimitive {
    public static final VMByte ZERO = new VMByte((byte) 0x00);

    private VMByte(final byte value) {
        super(PrimitiveTypes.BYTE, value);
    }
}
