package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

public sealed class VMInteger extends AbstractVMPrimitive permits VMByte, VMChar, VMShort {
    public static final VMInteger ZERO = new VMInteger(0);

    protected VMInteger(VMType type, final int value) {
        super(type, value);
    }

    public VMInteger(final int value) {
        super(VMType.INTEGER, value);
    }

    @NotNull
    public VMInteger add(@NotNull VMInteger other) {
        return new VMInteger(this.asNumber().intValue() + other.asNumber().intValue());
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        return other instanceof VMInteger;
    }

    @Override
    public String toString() {
        return String.valueOf(this.asNumber().intValue());
    }

    public @NotNull VMInteger sub(VMInteger val1) {
        return new VMInteger(this.asNumber().intValue() - val1.asNumber().intValue());
    }

    public @NotNull VMInteger mul(VMInteger val1) {
        return new VMInteger(this.asNumber().intValue() * val1.asNumber().intValue());
    }

    public @NotNull VMInteger div(VMInteger val1) {
        return new VMInteger(this.asNumber().intValue() / val1.asNumber().intValue());
    }

    public @NotNull VMInteger rem(VMInteger val1) {
        return new VMInteger(this.asNumber().intValue() % val1.asNumber().intValue());
    }

    public @NotNull VMInteger neg(VMInteger val1) {
        return new VMInteger(-val1.asNumber().intValue());
    }

    public @NotNull VMInteger shl(VMInteger val1) {
        return new VMInteger(this.asNumber().intValue() << val1.asNumber().intValue());
    }

    public @NotNull VMInteger shr(VMInteger val1) {
        return new VMInteger(this.asNumber().intValue() >> val1.asNumber().intValue());
    }

    public @NotNull VMInteger ushr(VMInteger val1) {
        return new VMInteger(this.asNumber().intValue() >>> val1.asNumber().intValue());
    }

    public @NotNull VMInteger and(VMInteger val1) {
        return new VMInteger(this.asNumber().intValue() & val1.asNumber().intValue());
    }

    public @NotNull VMInteger or(VMInteger val1) {
        return new VMInteger(this.asNumber().intValue() | val1.asNumber().intValue());
    }

    public @NotNull VMValue xor(VMInteger val1) {
        return new VMInteger(this.asNumber().intValue() ^ val1.asNumber().intValue());
    }
}
