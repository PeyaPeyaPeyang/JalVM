package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

public final class VMDouble extends AbstractVMPrimitive {
    public static final VMDouble ZERO = new VMDouble(0d);

    public VMDouble(final double value) {
        super(PrimitiveTypes.DOUBLE, value);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        return other instanceof VMDouble;
    }

    @Override
    public String toString() {
        return String.format("%f", this.asNumber().doubleValue()) + "d";
    }
}
