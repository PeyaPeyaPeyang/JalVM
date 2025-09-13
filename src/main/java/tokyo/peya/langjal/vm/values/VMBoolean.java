package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threading.VMThread;

import java.util.HashMap;

public final class VMBoolean extends VMInteger
{
    private static final HashMap<@NotNull JalVM, VMBoolean> TRUES = new HashMap<>();
    private static final HashMap<@NotNull JalVM, VMBoolean> FALSES = new HashMap<>();

    private final JalVM vm;

    private VMBoolean(@NotNull VMComponent component, final boolean value)
    {
        super(component, VMType.of(component, PrimitiveTypes.BOOLEAN), value ? 1: 0);
        this.vm = component.getVM();
    }

    public static VMBoolean ofTrue(@NotNull VMComponent component)
    {
        return TRUES.computeIfAbsent(component.getVM(), k -> new VMBoolean(component, true));
    }

    public static VMBoolean ofFalse(@NotNull VMComponent component)
    {
        return FALSES.computeIfAbsent(component.getVM(), k -> new VMBoolean(component, false));
    }

    @Override
    public int identityHashCode()
    {
        return Boolean.hashCode(this.asBoolean());
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        if (other instanceof VMBoolean)
            return true;

        if (other instanceof VMInteger intVal)
        {
            int value = intVal.asNumber().intValue();
            return value == 0 || value == 1;
        }

        return false;
    }

    @Override
    public @NotNull VMBoolean cloneValue()
    {
        return this;
    }

    @Override
    public @NotNull String toString()
    {
        return this.asNumber().intValue() == 0 ? "false": "true";
    }

    public static VMBoolean of(@NotNull VMComponent component, final boolean value)
    {
        return value ? ofTrue(component) : ofFalse(component);
    }

    @Override
    public VMValue conformValue(@NotNull VMType<?> expectedType)
    {
        if (expectedType.getType() == PrimitiveTypes.BOOLEAN)
            return this;
        else if (expectedType.getType() == PrimitiveTypes.INT)
            return new VMInteger(this.vm, this.asNumber().intValue());

        return super.conformValue(expectedType);
    }

    public boolean asBoolean()
    {
        return this.asNumber().intValue() != 0;
    }
}
