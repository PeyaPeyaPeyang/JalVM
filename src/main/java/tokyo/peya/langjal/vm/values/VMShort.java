package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;

public final class VMShort extends VMInteger
{
    public VMShort(@NotNull VMComponent component, final short value)
    {
        super(component, VMType.of(component, PrimitiveTypes.SHORT), value);
    }

    public static VMShort ofZero(@NotNull VMComponent component)
    {
        return new VMShort(component, (short) 0);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        if (other instanceof VMShort)
            return true;

        if (other instanceof VMInteger intVal)
        {
            int value = intVal.asNumber().intValue();
            return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
        }

        return false;
    }

    @Override
    public @NotNull VMShort cloneValue()
    {
        return this;
    }

    @Override
    public @NotNull String toString()
    {
        return String.valueOf(this.asNumber().shortValue()) + "s";
    }
}
