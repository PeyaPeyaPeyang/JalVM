package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public final class VMChar extends AbstractVMPrimitive {
    public static final VMChar ZERO = new VMChar(BigDecimal.ZERO);

    private VMChar(final BigDecimal value) {
        super(PrimitiveTypes.DOUBLE, value);
    }

    public VMChar(final char value) {
        this(BigDecimal.valueOf(value));
    }
}
