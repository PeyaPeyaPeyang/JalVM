package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

public interface VMValue
{
    @NotNull
    VMType type();

    boolean isCompatibleTo(@NotNull VMValue other);

    default boolean isCategory2()
    {
        return this.type().getType().getCategory() == 2;
    }

    String toString();

    default Object toJavaObject()
    {
        if (this instanceof VMPrimitive<?>)
            return ((VMPrimitive<?>) this).asNumber();
        else
            throw new VMPanic("Cannot convert " + this.getClass().getName());
    }

    static VMValue fromJavaObject(@NotNull Object obj)
    {
        return switch (obj)
        {
            case VMValue vmValue -> vmValue;
            case Integer i -> new VMInteger(i);
            case Long l -> new VMLong(l);
            case Double v -> new VMDouble(v);
            case Boolean b -> b ? VMBoolean.TRUE: VMBoolean.FALSE;
            default -> throw new VMPanic("Unsupported type: " + obj.getClass().getName());
        };
    }
}
