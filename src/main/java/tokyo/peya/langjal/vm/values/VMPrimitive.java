package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;

public interface VMPrimitive extends VMValue
{
    @NotNull
    Number asNumber();

    @Override
    @NotNull VMPrimitive cloneValue();
}
