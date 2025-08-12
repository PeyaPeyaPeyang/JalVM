package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public final class VMChar extends VMInteger {
    public static final VMChar ZERO = new VMChar((char) 0);

    private VMChar(final char value) {
        super(PrimitiveTypes.CHAR, value);
    }

    @Override
    public String toString() {
        return "'" + (char) this.asNumber().intValue() + "'";
    }
}
