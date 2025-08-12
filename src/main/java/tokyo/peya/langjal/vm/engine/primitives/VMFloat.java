package tokyo.peya.langjal.vm.engine.primitives;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public final class VMFloat extends AbstractVMPrimitive {
    public VMFloat(@NotNull BigDecimal value) {
        super(PrimitiveTypes.FLOAT, value);
    }
}
