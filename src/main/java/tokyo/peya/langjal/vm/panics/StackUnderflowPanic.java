package tokyo.peya.langjal.vm.panics;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.values.VMObject;

public class StackUnderflowPanic extends VMPanic
{
    public StackUnderflowPanic(String message,
                               @Nullable VMObject associatedThrowable)
    {
        super(message, associatedThrowable);
    }

    public StackUnderflowPanic(String message, Throwable cause,
                               @Nullable VMObject associatedThrowable)
    {
        super(message, cause, associatedThrowable);
    }

    public StackUnderflowPanic(Throwable cause,
                               @Nullable VMObject associatedThrowable)
    {
        super(cause, associatedThrowable);
    }

    public StackUnderflowPanic(String message)
    {
        super(message);
    }

    public StackUnderflowPanic(String message, Throwable cause)
    {
        super(message, cause);
    }

    public StackUnderflowPanic(Throwable cause)
    {
        super(cause);
    }
}
