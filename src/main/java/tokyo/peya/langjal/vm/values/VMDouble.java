package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public final class VMDouble extends AbstractVMPrimitive {
    public static final VMDouble ZERO = new VMDouble(0d);

    public VMDouble(final double value) {
        super(VMType.DOUBLE, value);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        return other instanceof VMDouble;
    }

    @Override
    public String toString() {
        return String.format("%f", this.asNumber().doubleValue()) + "d";
    }

    public @NotNull VMValue add(@NotNull VMDouble l2) {
        return new VMDouble(this.asNumber().doubleValue() + l2.asNumber().doubleValue());
    }

    public @NotNull VMValue sub(VMDouble val1) {
        return new VMDouble(this.asNumber().doubleValue() - val1.asNumber().doubleValue());
    }

    public @NotNull VMValue mul(VMDouble val2) {
        return new VMDouble(this.asNumber().doubleValue() * val2.asNumber().doubleValue());
    }

    public @NotNull VMValue div(VMDouble val1) {
        return new VMDouble(this.asNumber().doubleValue() / val1.asNumber().doubleValue());
    }

    public @NotNull VMValue rem(VMDouble val2) {
        return new VMDouble(this.asNumber().doubleValue() % val2.asNumber().doubleValue());
    }

    public @NotNull VMValue neg(VMDouble val1) {
        return new VMDouble(-val1.asNumber().doubleValue());
    }
}
