package tokyo.peya.langjal.vm.engine.primitives;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public final class VMShort extends AbstractVMPrimitive {
    public VMShort(final BigDecimal value) {
        super(PrimitiveTypes.SHORT, value);
    }

    public VMShort(final float value) {
        super(PrimitiveTypes.SHORT, BigDecimal.valueOf(value));
    }
}
