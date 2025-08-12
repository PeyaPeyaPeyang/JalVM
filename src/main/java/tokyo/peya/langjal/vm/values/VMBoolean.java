package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public final class VMBoolean extends AbstractVMPrimitive {
    public static final VMBoolean TRUE = new VMBoolean(true);
    public static final VMBoolean FALSE = new VMBoolean(false);


    private VMBoolean(final boolean value) {
        super(PrimitiveTypes.BOOLEAN, value ? new BigDecimal(1) : BigDecimal.ZERO);
    }
}
