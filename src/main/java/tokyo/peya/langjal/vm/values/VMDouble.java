package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;

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

    public @NotNull VMDouble add(@NotNull VMDouble l2) {
        return new VMDouble(this.asNumber().doubleValue() + l2.asNumber().doubleValue());
    }

    public @NotNull VMDouble sub(VMDouble val1) {
        return new VMDouble(this.asNumber().doubleValue() - val1.asNumber().doubleValue());
    }

    public @NotNull VMDouble mul(VMDouble val2) {
        return new VMDouble(this.asNumber().doubleValue() * val2.asNumber().doubleValue());
    }

    public @NotNull VMDouble div(VMDouble val1) {
        if (val1.asNumber().doubleValue() == 0.0)
            throw new IllegalOperandPanic("Division by zero");
        return new VMDouble(this.asNumber().doubleValue() / val1.asNumber().doubleValue());
    }

    public @NotNull VMDouble rem(VMDouble val2) {
        return new VMDouble(this.asNumber().doubleValue() % val2.asNumber().doubleValue());
    }

    public @NotNull VMDouble neg() {
        return new VMDouble(-this.asNumber().doubleValue());
    }
}
