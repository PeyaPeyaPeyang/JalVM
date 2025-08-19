package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

public interface VMReferenceValue extends VMValue
{
    @Override
    default VMValue conformValue(@NotNull VMType<?> expectedType)
    {
        if (expectedType.isPrimitive())
            throw new VMPanic("Cannot conform a reference value to a primitive type: " + expectedType.getTypeDescriptor());

        if (expectedType.isAssignableFrom(this.type()))
            return this;

        throw new VMPanic("Cannot conform a reference value to the expected type: " + this.type().getTypeDescriptor() + " to " + expectedType.getTypeDescriptor());
    }
}
