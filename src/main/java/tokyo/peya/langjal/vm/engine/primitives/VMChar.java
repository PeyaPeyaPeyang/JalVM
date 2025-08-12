package tokyo.peya.langjal.vm.engine.primitives;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public final class VMChar extends AbstractVMPrimitive {
    public VMChar(final BigDecimal value) {
        super(PrimitiveTypes.DOUBLE, value);
    }
}
