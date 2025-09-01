package tokyo.peya.langjal.vm.panics;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.values.VMObject;

public class UnknownInstructionPanic extends VMPanic
{
    public UnknownInstructionPanic(String message,
                                   @Nullable VMObject associatedThrowable)
    {
        super(message, associatedThrowable);
    }

    public UnknownInstructionPanic(String message, Throwable cause,
                                   @Nullable VMObject associatedThrowable)
    {
        super(message, cause, associatedThrowable);
    }

    public UnknownInstructionPanic(Throwable cause,
                                   @Nullable VMObject associatedThrowable)
    {
        super(cause, associatedThrowable);
    }

    public UnknownInstructionPanic(String message)
    {
        super(message);
    }

    public UnknownInstructionPanic(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnknownInstructionPanic(Throwable cause)
    {
        super(cause);
    }
}
