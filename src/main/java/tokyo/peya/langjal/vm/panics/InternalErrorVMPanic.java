package tokyo.peya.langjal.vm.panics;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.values.VMObject;

public class InternalErrorVMPanic extends VMPanic
{
    public InternalErrorVMPanic(String message,
                                @Nullable VMObject associatedThrowable)
    {
        super(message, associatedThrowable);
    }

    public InternalErrorVMPanic(String message, Throwable cause,
                                @Nullable VMObject associatedThrowable)
    {
        super(message, cause, associatedThrowable);
    }

    public InternalErrorVMPanic(Throwable cause,
                                @Nullable VMObject associatedThrowable)
    {
        super(cause, associatedThrowable);
    }

    public InternalErrorVMPanic(String message)
    {
        super(message);
    }

    public InternalErrorVMPanic(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InternalErrorVMPanic(Throwable cause)
    {
        super(cause);
    }
}
