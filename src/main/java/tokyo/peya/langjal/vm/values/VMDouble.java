package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMDouble extends AbstractVMPrimitive {
    public static final VMDouble ZERO = new VMDouble(0d);

    public VMDouble(final double value) {
        super(PrimitiveTypes.DOUBLE, value);
    }
}
