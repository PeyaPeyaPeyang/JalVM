package tokyo.peya.langjal.vm.panics;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.values.VMObject;

public class NoReferencePanic extends VMPanic
{
    public NoReferencePanic(String message,
                            @Nullable VMObject associatedThrowable)
    {
        super(message, associatedThrowable);
    }

    public NoReferencePanic(String message, Throwable cause,
                            @Nullable VMObject associatedThrowable)
    {
        super(message, cause, associatedThrowable);
    }

    public NoReferencePanic(Throwable cause,
                            @Nullable VMObject associatedThrowable)
    {
        super(cause, associatedThrowable);
    }

    public NoReferencePanic(String message)
    {
        super(message);
    }

    public NoReferencePanic(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NoReferencePanic(Throwable cause)
    {
        super(cause);
    }
}
