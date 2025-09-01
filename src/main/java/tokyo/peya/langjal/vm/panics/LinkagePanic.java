package tokyo.peya.langjal.vm.panics;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.values.VMObject;

public class LinkagePanic extends VMPanic
{
    public LinkagePanic(String message,
                        @Nullable VMObject associatedThrowable)
    {
        super(message, associatedThrowable);
    }

    public LinkagePanic(String message, Throwable cause,
                        @Nullable VMObject associatedThrowable)
    {
        super(message, cause, associatedThrowable);
    }

    public LinkagePanic(Throwable cause,
                        @Nullable VMObject associatedThrowable)
    {
        super(cause, associatedThrowable);
    }

    public LinkagePanic(String message)
    {
        super(message);
    }

    public LinkagePanic(String message, Throwable cause)
    {
        super(message, cause);
    }

    public LinkagePanic(Throwable cause)
    {
        super(cause);
    }
}
