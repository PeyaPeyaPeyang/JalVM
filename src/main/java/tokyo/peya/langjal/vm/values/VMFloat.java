package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public final class VMFloat extends AbstractVMPrimitive {
    public static final VMFloat ZERO = new VMFloat(0f);

    public VMFloat(final float value) {
        super(VMType.FLOAT, value);
    }

    @Override
    public boolean isCompatibleTo(@NotNull VMValue other) {
        return other instanceof VMFloat;
    }

    @Override
    public String toString() {
        return String.format("%f", this.asNumber().floatValue()) + "f";
    }

    public @NotNull VMFloat add(VMFloat l2) {
        return new VMFloat(this.asNumber().floatValue() + l2.asNumber().floatValue());
    }

    public @NotNull VMFloat sub(VMFloat val1) {
        return new VMFloat(this.asNumber().floatValue() - val1.asNumber().floatValue());
    }

    public @NotNull VMFloat mul(VMFloat val1) {
        return new VMFloat(this.asNumber().floatValue() * val1.asNumber().floatValue());
    }

    public @NotNull VMFloat div(VMFloat val1) {
        return new VMFloat(this.asNumber().floatValue() / val1.asNumber().floatValue());
    }

    public @NotNull VMFloat rem(VMFloat val1) {
        return new VMFloat(this.asNumber().floatValue() % val1.asNumber().floatValue());
    }

    public @NotNull VMFloat neg(VMFloat val1) {
        return new VMFloat(-val1.asNumber().floatValue());
    }
}
