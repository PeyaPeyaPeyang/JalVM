package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;

public final class VMLong extends AbstractVMPrimitive
{
    private final JalVM vm;

    public VMLong(@NotNull VMComponent component, final long value)
    {
        super(VMType.of(component, PrimitiveTypes.LONG), value);
        this.vm = component.getVM();
    }


    public static @NotNull VMLong ofZero(@NotNull VMComponent component)
    {
        return new VMLong(component, 0L);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMLong;
    }

    @Override
    public @NotNull VMLong cloneValue()
    {
        return new VMLong( this.vm, this.asNumber().longValue());
    }


    @Override
    public @NotNull String toString()
    {
        return String.valueOf(this.asNumber().longValue());
    }

    public @NotNull VMLong add(VMLong l2)
    {
        return new VMLong(this.vm, this.asNumber().longValue() + l2.asNumber().longValue());
    }

    public @NotNull VMLong sub(VMLong val1)
    {
        return new VMLong(this.vm, this.asNumber().longValue() - val1.asNumber().longValue());
    }

    public @NotNull VMLong mul(VMLong val1)
    {
        return new VMLong(this.vm, this.asNumber().longValue() * val1.asNumber().longValue());
    }

    public @NotNull VMLong div(VMLong val1)
    {
        if (val1.asNumber().longValue() == 0L)
            throw new IllegalOperandPanic("Division by zero");
        return new VMLong(this.vm, this.asNumber().longValue() / val1.asNumber().longValue());
    }

    public @NotNull VMLong rem(VMLong val1)
    {
        return new VMLong(this.vm, this.asNumber().longValue() % val1.asNumber().longValue());
    }

    public @NotNull VMLong neg()
    {
        return new VMLong(this.vm, -this.asNumber().longValue());
    }

    public @NotNull VMLong shl(VMLong val1)
    {
        return new VMLong(this.vm, this.asNumber().longValue() << val1.asNumber().longValue());
    }

    public @NotNull VMLong shr(VMLong val1)
    {
        return new VMLong(this.vm, this.asNumber().longValue() >> val1.asNumber().longValue());
    }

    public @NotNull VMLong lushr(VMLong val1)
    {
        return new VMLong(this.vm, this.asNumber().longValue() >>> val1.asNumber().longValue());
    }

    public @NotNull VMLong and(VMLong val1)
    {
        return new VMLong(this.vm, this.asNumber().longValue() & val1.asNumber().longValue());
    }

    public @NotNull VMLong or(VMLong val1)
    {
        return new VMLong(this.vm, this.asNumber().longValue() | val1.asNumber().longValue());
    }

    public @NotNull VMLong xor(VMLong val1)
    {
        return new VMLong(this.vm, this.asNumber().longValue() ^ val1.asNumber().longValue());
    }
}
