package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMFrame;

public final class VMByte extends VMInteger
{
    private final JalVM vm;

    public VMByte(@NotNull JalVM vm, final byte value)
    {
        super(vm, VMType.of(vm, PrimitiveTypes.BYTE), value);
        this.vm = vm;
    }

    public VMByte(@NotNull VMFrame frame, final byte value)
    {
        this(frame.getVm(), value);
    }

    public static VMByte ofZero(@NotNull JalVM vm)
    {
        return new VMByte(vm, (byte) 0);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        if (other instanceof VMByte)
            return true;

        if (other instanceof VMInteger intVal)
        {
            int value = intVal.asNumber().intValue();
            return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
        }

        return false;
    }

    @Override
    public @NotNull VMByte cloneValue()
    {
        return new VMByte(this.vm, this.asNumber().byteValue());
    }

    @Override
    public @NotNull String toString()
    {
        return String.format("0x%02X", this.asNumber().byteValue());
    }
}
