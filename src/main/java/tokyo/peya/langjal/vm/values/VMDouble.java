package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;

public final class VMDouble extends AbstractVMPrimitive
{
    public static final VMDouble ZERO = new VMDouble(0d);
    public static final VMDouble NAN = new VMDouble(Double.NaN);
    public static final VMDouble POSITIVE_INFINITY = new VMDouble(Double.POSITIVE_INFINITY);
    public static final VMDouble NEGATIVE_INFINITY = new VMDouble(Double.NEGATIVE_INFINITY);


    public VMDouble(final double value)
    {
        super(VMType.DOUBLE, value);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMDouble;
    }

    @Override
    public @NotNull String toString()
    {
        return String.format("%f", this.asNumber().doubleValue()) + "d";
    }

    @Override
    public @NotNull VMDouble cloneValue()
    {
        return new VMDouble(this.asNumber().doubleValue());
    }

    public boolean isNaN()
    {
        return Double.isNaN(this.asNumber().doubleValue());
    }

    public boolean isInfinite()
    {
        return Double.isInfinite(this.asNumber().doubleValue());
    }

    public @NotNull VMDouble add(@NotNull VMDouble l2)
    {
        return new VMDouble(this.asNumber().doubleValue() + l2.asNumber().doubleValue());
    }

    public @NotNull VMDouble sub(VMDouble val1)
    {
        return new VMDouble(this.asNumber().doubleValue() - val1.asNumber().doubleValue());
    }

    public @NotNull VMDouble mul(VMDouble val2)
    {
        return new VMDouble(this.asNumber().doubleValue() * val2.asNumber().doubleValue());
    }

    public @NotNull VMDouble div(VMDouble val1)
    {
        if (val1.asNumber().doubleValue() == 0.0)
            throw new IllegalOperandPanic("Division by zero");
        return new VMDouble(this.asNumber().doubleValue() / val1.asNumber().doubleValue());
    }

    public @NotNull VMDouble rem(VMDouble val2)
    {
        return new VMDouble(this.asNumber().doubleValue() % val2.asNumber().doubleValue());
    }

    public @NotNull VMDouble neg()
    {
        return new VMDouble(-this.asNumber().doubleValue());
    }
}
