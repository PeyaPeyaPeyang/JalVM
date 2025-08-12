package tokyo.peya.langjal.vm.engine.primitives;

import com.sun.jdi.PrimitiveType;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public abstract class AbstractVMPrimitive implements VMPrimitive {
    private final PrimitiveTypes type;
    private final BigDecimal rawValue;

    protected AbstractVMPrimitive(@NotNull PrimitiveTypes type, @NotNull BigDecimal rawValue) {
        this.type = type;
        this.rawValue = rawValue;
    }

    @Override
    public PrimitiveTypes getType() {
        return this.type;
    }

    @Override
    public BigDecimal asBigDecimal() {
        return this.rawValue;
    }
}
