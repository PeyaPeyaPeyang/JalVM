package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;

public final class VMFloat extends AbstractVMPrimitive
{
    private final JalVM vm;

    public VMFloat(@NotNull VMComponent component, final float value)
    {
        super(VMType.of(component, PrimitiveTypes.FLOAT), value);
        this.vm = component.getVM();
    }

    public static @NotNull VMFloat ofZero(@NotNull VMComponent component)
    {
        return new VMFloat(component, 0.0f);
    }

    public static @NotNull VMFloat ofNaN(@NotNull VMComponent component)
    {
        return new VMFloat(component, Float.NaN);
    }

    public static @NotNull VMFloat ofPositiveInfinity(@NotNull VMComponent component)
    {
        return new VMFloat(component, Float.POSITIVE_INFINITY);
    }

    public static @NotNull VMFloat ofNegativeInfinity(@NotNull VMComponent component)
    {
        return new VMFloat(component, Float.NEGATIVE_INFINITY);
    }

    @Override
    public int identityHashCode()
    {
        return Float.hashCode(this.asNumber().floatValue());
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMFloat;
    }

    @Override
    public @NotNull VMFloat cloneValue()
    {
        return new VMFloat(this.vm, this.asNumber().floatValue());
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
    public @NotNull String toString()
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
        return new VMFloat(this.vm, this.asNumber().floatValue() + l2.asNumber().floatValue());
    }

    public @NotNull VMFloat sub(VMFloat val1)
    {
        return new VMFloat(this.vm, this.asNumber().floatValue() - val1.asNumber().floatValue());
    }

    public @NotNull VMFloat mul(VMFloat val1)
    {
        return new VMFloat(this.vm, this.asNumber().floatValue() * val1.asNumber().floatValue());
    }

    public @NotNull VMFloat div(VMFloat val1)
    {
        if (val1.asNumber().floatValue() == 0.0f)
            throw new IllegalOperandPanic("Division by zero");
        return new VMFloat(this.vm, this.asNumber().floatValue() / val1.asNumber().floatValue());
    }

    public @NotNull VMFloat rem(VMFloat val1)
    {
        return new VMFloat(this.vm, this.asNumber().floatValue() % val1.asNumber().floatValue());
    }

    public @NotNull VMFloat neg()
    {
        return new VMFloat(this.vm, -this.asNumber().floatValue());
    }
}
