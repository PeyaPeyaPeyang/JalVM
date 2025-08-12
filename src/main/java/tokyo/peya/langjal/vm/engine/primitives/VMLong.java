package tokyo.peya.langjal.vm.engine.primitives;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public final class VMLong extends AbstractVMPrimitive {
    public VMLong(@NotNull BigDecimal value) {
        super(PrimitiveTypes.LONG, value);
    }
}
