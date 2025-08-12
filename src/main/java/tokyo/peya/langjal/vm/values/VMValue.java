package tokyo.peya.langjal.vm.values;

import com.sun.jdi.PrimitiveType;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.compiler.jvm.Type;

public interface VMValue {
    @NotNull
    Type getType();

    boolean isCompatibleTo(@NotNull VMValue other);

    default boolean isCategory2()
    {
        return this.getType().getCategory() == 2;
    }
}
