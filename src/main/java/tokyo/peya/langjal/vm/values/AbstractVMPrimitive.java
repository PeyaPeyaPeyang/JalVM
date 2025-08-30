package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMPrimitiveClass;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

public abstract class AbstractVMPrimitive implements VMPrimitive
{
    private final VMType<?> type;
    private final Number rawValue;

    protected AbstractVMPrimitive(@NotNull VMType<?> type, @NotNull Number rawValue)
    {
        this.type = type;
        this.rawValue = rawValue;
    }

    @Override
    public @NotNull VMType<?> type()
    {
        return this.type;
    }

    @Override
    public @NotNull Number asNumber()
    {
        return this.rawValue;
    }

    @Override
    public VMValue conformValue(@NotNull VMType<?> expectedType)
    {
        if (this.type().equals(expectedType))
            return this;

        VMType<?> currentType = this.type();
        VMClass currentClass = currentType.getLinkedClass();
        if(!(currentClass instanceof VMPrimitiveClass primitive))
            throw new VMPanic("Current type is not a primitive class: " + currentType);

        VMClass expectedClass = expectedType.getLinkedClass();
        if (!expectedClass.isLinked())
            expectedClass.linkClass(currentClass.getClassLoader());

        if (primitive.equals(expectedClass))
            return this;
        else if (expectedClass.isAssignableFrom(primitive.getBoxedClass()))
            return primitive.createBoxedInstance(this);

        throw new VMPanic("Cannot conform primitive type " + this.type() + " to " + expectedType);
    }
}
