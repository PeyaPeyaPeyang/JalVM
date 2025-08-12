package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMInteger extends AbstractVMPrimitive {
    public static final VMInteger ZERO = new VMInteger(0);

    private VMInteger(@NotNull int value) {
        super(PrimitiveTypes.INT, value);
    }
}
