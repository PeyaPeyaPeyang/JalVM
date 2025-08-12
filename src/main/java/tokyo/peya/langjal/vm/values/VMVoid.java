package tokyo.peya.langjal.vm.values;

import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.compiler.jvm.Type;

public class VMVoid implements VMValue {
    public static final VMVoid INSTANCE = new VMVoid();

    private VMVoid() {
    }

    @Override
    public Type getType() {
        return PrimitiveTypes.VOID;
    }
}
