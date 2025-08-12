package tokyo.peya.langjal.vm.engine.primitives;

import com.sun.jdi.PrimitiveType;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public final class VMByte extends AbstractVMPrimitive {
    public VMByte(@NotNull BigDecimal value) {
        super(PrimitiveTypes.BYTE, value);
    }
}
