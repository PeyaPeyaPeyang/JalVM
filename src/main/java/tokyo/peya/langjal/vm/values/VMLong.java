package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.IllegalOperandPanic;

public final class VMLong extends AbstractVMPrimitive
{
    public static final VMLong ZERO = new VMLong(0);

    public VMLong(final long value)
    {
        super(VMType.LONG, value);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other)
    {
        return other instanceof VMLong;
    }

    @Override
    public @NotNull VMLong cloneValue()
    {
        return new VMLong(this.asNumber().longValue());
    }


    @Override
    public @NotNull String toString()
    {
        return String.valueOf(this.asNumber().longValue());
    }

    public @NotNull VMLong add(VMLong l2)
    {
        return new VMLong(this.asNumber().longValue() + l2.asNumber().longValue());
    }

    public @NotNull VMLong sub(VMLong val1)
    {
        return new VMLong(this.asNumber().longValue() - val1.asNumber().longValue());
    }

    public @NotNull VMLong mul(VMLong val1)
    {
        return new VMLong(this.asNumber().longValue() * val1.asNumber().longValue());
    }

    public @NotNull VMLong div(VMLong val1)
    {
        if (val1.asNumber().longValue() == 0L)
            throw new IllegalOperandPanic("Division by zero");
        return new VMLong(this.asNumber().longValue() / val1.asNumber().longValue());
    }

    public @NotNull VMLong rem(VMLong val1)
    {
        return new VMLong(this.asNumber().longValue() % val1.asNumber().longValue());
    }

    public @NotNull VMLong neg()
    {
        return new VMLong(-this.asNumber().longValue());
    }

    public @NotNull VMLong shl(VMLong val1)
    {
        return new VMLong(this.asNumber().longValue() << val1.asNumber().longValue());
    }

    public @NotNull VMLong shr(VMLong val1)
    {
        return new VMLong(this.asNumber().longValue() >> val1.asNumber().longValue());
    }

    public @NotNull VMLong lushr(VMLong val1)
    {
        return new VMLong(this.asNumber().longValue() >>> val1.asNumber().longValue());
    }

    public @NotNull VMLong and(VMLong val1)
    {
        return new VMLong(this.asNumber().longValue() & val1.asNumber().longValue());
    }

    public @NotNull VMLong or(VMLong val1)
    {
        return new VMLong(this.asNumber().longValue() | val1.asNumber().longValue());
    }

    public @NotNull VMLong xor(VMLong val1)
    {
        return new VMLong(this.asNumber().longValue() ^ val1.asNumber().longValue());
    }
}
