package tokyo.peya.langjal.vm.panics;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.values.VMObject;

public class IllegalOperandPanic extends VMPanic
{
    public IllegalOperandPanic(String message,
                               @Nullable VMObject associatedThrowable)
    {
        super(message, associatedThrowable);
    }

    public IllegalOperandPanic(String message, Throwable cause,
                               @Nullable VMObject associatedThrowable)
    {
        super(message, cause, associatedThrowable);
    }

    public IllegalOperandPanic(Throwable cause,
                               @Nullable VMObject associatedThrowable)
    {
        super(cause, associatedThrowable);
    }

    public IllegalOperandPanic(String message)
    {
        super(message);
    }

    public IllegalOperandPanic(String message, Throwable cause)
    {
        super(message, cause);
    }

    public IllegalOperandPanic(Throwable cause)
    {
        super(cause);
    }
}
