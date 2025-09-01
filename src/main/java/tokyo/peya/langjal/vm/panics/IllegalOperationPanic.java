package tokyo.peya.langjal.vm.panics;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.values.VMObject;

public class IllegalOperationPanic extends VMPanic
{
    public IllegalOperationPanic(String message,
                                 @Nullable VMObject associatedThrowable)
    {
        super(message, associatedThrowable);
    }

    public IllegalOperationPanic(String message, Throwable cause,
                                 @Nullable VMObject associatedThrowable)
    {
        super(message, cause, associatedThrowable);
    }

    public IllegalOperationPanic(Throwable cause,
                                 @Nullable VMObject associatedThrowable)
    {
        super(cause, associatedThrowable);
    }

    public IllegalOperationPanic(String message)
    {
        super(message);
    }

    public IllegalOperationPanic(String message, Throwable cause)
    {
        super(message, cause);
    }

    public IllegalOperationPanic(Throwable cause)
    {
        super(cause);
    }
}
