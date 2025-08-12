package tokyo.peya.langjal.vm.engine.primitives;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;

import java.math.BigDecimal;

public interface VMPrimitive {
    PrimitiveTypes getType();

    BigDecimal asBigDecimal();
}
