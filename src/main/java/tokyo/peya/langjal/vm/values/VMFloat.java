package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMFloat extends AbstractVMPrimitive {
    public static final VMFloat ZERO = new VMFloat(0f);

    private VMFloat(final float value) {
        super(PrimitiveTypes.DOUBLE, value);
    }
}
