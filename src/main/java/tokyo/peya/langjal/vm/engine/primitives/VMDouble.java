package tokyo.peya.langjal.vm.engine.primitives;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public final class VMDouble extends AbstractVMPrimitive {
    public VMDouble(@NotNull BigDecimal value) {
        super(PrimitiveTypes.DOUBLE, value);
    }
}
