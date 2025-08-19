package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;

public final class VMFloat extends AbstractVMPrimitive
{
    public static final VMFloat ZERO = new VMFloat(0f);
    public static final VMFloat NAN = new VMFloat(Float.NaN);
    public static final VMFloat POSITIVE_INFINITY = new VMFloat(Float.POSITIVE_INFINITY);
    public static final VMFloat NEGATIVE_INFINITY = new VMFloat(Float.NEGATIVE_INFINITY);

    public VMFloat(final float value)
    {
        super(VMType.FLOAT, value);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMFloat;
    }

    public boolean isNaN()
    {
        return Float.isNaN(this.asNumber().floatValue());
    }

    public boolean isInfinite()
    {
        return Float.isInfinite(this.asNumber().floatValue());
    }

    @Override
    public String toString()
    {
        float v = this.asNumber().floatValue();
        if (Float.isNaN(v))
            return "NaN";
        if (Float.isInfinite(v))
            return v > 0 ? "Infinity" : "-Infinity";
        return String.format("%f", v) + "f";
    }

    public @NotNull VMFloat add(VMFloat l2)
    {
        return new VMFloat(this.asNumber().floatValue() + l2.asNumber().floatValue());
    }

    public @NotNull VMFloat sub(VMFloat val1)
    {
        return new VMFloat(this.asNumber().floatValue() - val1.asNumber().floatValue());
    }

    public @NotNull VMFloat mul(VMFloat val1)
    {
        return new VMFloat(this.asNumber().floatValue() * val1.asNumber().floatValue());
    }

    public @NotNull VMFloat div(VMFloat val1)
    {
        if (val1.asNumber().floatValue() == 0.0f)
            throw new IllegalOperandPanic("Division by zero");
        return new VMFloat(this.asNumber().floatValue() / val1.asNumber().floatValue());
    }

    public @NotNull VMFloat rem(VMFloat val1)
    {
        return new VMFloat(this.asNumber().floatValue() % val1.asNumber().floatValue());
    }

    public @NotNull VMFloat neg()
    {
        return new VMFloat(-this.asNumber().floatValue());
    }
}
