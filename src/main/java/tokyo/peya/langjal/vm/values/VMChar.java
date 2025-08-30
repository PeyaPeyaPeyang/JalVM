package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;

public final class VMChar extends VMInteger
{
    public VMChar(@NotNull VMComponent component, final char value)
    {
        super(component, VMType.of(component, PrimitiveTypes.CHAR), value);
    }

    public static VMChar ofZero(@NotNull VMComponent component)
    {
        return new VMChar(component, (char) 0);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        if (other instanceof VMChar)
            return true;

        if (other instanceof VMInteger intVal)
        {
            int value = intVal.asNumber().intValue();
            return value >= Character.MIN_VALUE && value <= Character.MAX_VALUE;
        }

        return false;
    }

    @Override
    public @NotNull VMChar cloneValue()
    {
        return new VMChar(this.vm, (char) this.asNumber().intValue());
    }

    @Override
    public @NotNull String toString()
    {
        return "'" + (char) this.asNumber().intValue() + "'";
    }
}
