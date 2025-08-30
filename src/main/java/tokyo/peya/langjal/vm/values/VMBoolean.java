package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threading.VMThread;

import java.util.HashMap;

public final class VMBoolean extends AbstractVMPrimitive
{
    private static final HashMap<@NotNull JalVM, VMBoolean> TRUES = new HashMap<>();
    private static final HashMap<@NotNull JalVM, VMBoolean> FALSES = new HashMap<>();

    private final JalVM vm;

    private VMBoolean(@NotNull JalVM vm, final boolean value)
    {
        super(VMType.of(vm, PrimitiveTypes.BOOLEAN), value ? 1: 0);
        this.vm = vm;
    }

    public static VMBoolean ofTrue(@NotNull VMThread thread)
    {
        return ofTrue(thread.getVm());
    }

    public static VMBoolean ofFalse(@NotNull VMThread thread)
    {
        return ofFalse(thread.getVm());
    }

    public static VMBoolean ofTrue(@NotNull VMFrame frame)
    {
        return ofTrue(frame.getVm());
    }

    public static VMBoolean ofFalse(@NotNull VMFrame frame)
    {
        return ofFalse(frame.getVm());
    }

    public static VMBoolean ofTrue(@NotNull JalVM vm)
    {
        return TRUES.computeIfAbsent(vm, k -> new VMBoolean(vm, true));
    }

    public static VMBoolean ofFalse(@NotNull JalVM vm)
    {
        return FALSES.computeIfAbsent(vm, k -> new VMBoolean(vm, false));
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

    public static VMBoolean of(@NotNull JalVM vm, final boolean value)
    {
        return value ? ofTrue(vm) : ofFalse(vm);
    }

    public static VMBoolean of(@NotNull VMFrame frame, final boolean value)
    {
        return of(frame.getVm(), value);
    }

    public static VMBoolean of(@NotNull VMThread thread, final boolean value)
    {
        return of(thread.getVm(), value);
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
