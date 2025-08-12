package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public interface VMPrimitive<T> extends VMValue {
    @NotNull
    Number asNumber();
}
