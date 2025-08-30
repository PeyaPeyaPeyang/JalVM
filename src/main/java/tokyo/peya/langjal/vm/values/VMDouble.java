package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;

public final class VMDouble extends AbstractVMPrimitive
{
    private final JalVM vm;

    public VMDouble(@NotNull JalVM vm, final double value)
    {
        super(VMType.of(vm, PrimitiveTypes.DOUBLE), value);
        this.vm = vm;
    }

    public VMDouble(@NotNull VMFrame frame, final double value)
    {
        this(frame.getVm(), value);
    }

    public VMDouble(@NotNull VMThread thread, final double value)
    {
        this(thread.getVm(), value);
    }

    public static @NotNull VMDouble ofZero(@NotNull JalVM vm)
    {
        return new VMDouble(vm, 0.0);
    }

    public static @NotNull VMDouble ofNaN(@NotNull JalVM vm)
    {
        return new VMDouble(vm, Double.NaN);
    }

    public static @NotNull VMDouble ofPositiveInfinity(@NotNull JalVM vm)
    {
        return new VMDouble(vm, Double.POSITIVE_INFINITY);
    }

    public static @NotNull VMDouble ofNegativeInfinity(@NotNull JalVM vm)
    {
        return new VMDouble(vm, Double.NEGATIVE_INFINITY);
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
        return new VMDouble(this.vm, this.asNumber().doubleValue());
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
        return new VMDouble(this.vm, this.asNumber().doubleValue() + l2.asNumber().doubleValue());
    }

    public @NotNull VMDouble sub(VMDouble val1)
    {
        return new VMDouble(this.vm, this.asNumber().doubleValue() - val1.asNumber().doubleValue());
    }

    public @NotNull VMDouble mul(VMDouble val2)
    {
        return new VMDouble(this.vm, this.asNumber().doubleValue() * val2.asNumber().doubleValue());
    }

    public @NotNull VMDouble div(VMDouble val1)
    {
        if (val1.asNumber().doubleValue() == 0.0)
            throw new IllegalOperandPanic("Division by zero");
        return new VMDouble(this.vm, this.asNumber().doubleValue() / val1.asNumber().doubleValue());
    }

    public @NotNull VMDouble rem(VMDouble val2)
    {
        return new VMDouble(this.vm, this.asNumber().doubleValue() % val2.asNumber().doubleValue());
    }

    public @NotNull VMDouble neg()
    {
        return new VMDouble(this.vm, -this.asNumber().doubleValue());
    }
}
