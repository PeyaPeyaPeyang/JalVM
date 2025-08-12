package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMInteger extends AbstractVMPrimitive {
    public static final VMInteger ZERO = new VMInteger(0);

    private VMInteger(final int value) {
        super(PrimitiveTypes.INT, value);
    }
}
