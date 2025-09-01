package tokyo.peya.langjal.vm.panics;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.values.VMObject;

public class StackOverflowPanic extends VMPanic
{
    public StackOverflowPanic(String message,
                              @Nullable VMObject associatedThrowable)
    {
        super(message, associatedThrowable);
    }

    public StackOverflowPanic(String message, Throwable cause,
                              @Nullable VMObject associatedThrowable)
    {
        super(message, cause, associatedThrowable);
    }

    public StackOverflowPanic(Throwable cause,
                              @Nullable VMObject associatedThrowable)
    {
        super(cause, associatedThrowable);
    }

    public StackOverflowPanic(String message)
    {
        super(message);
    }

    public StackOverflowPanic(String message, Throwable cause)
    {
        super(message, cause);
    }

    public StackOverflowPanic(Throwable cause)
    {
        super(cause);
    }
}
