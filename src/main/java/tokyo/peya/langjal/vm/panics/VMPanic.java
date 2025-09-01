package tokyo.peya.langjal.vm.panics;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.values.VMObject;

@Getter
public class VMPanic extends RuntimeException
{
    @Nullable
    private final VMObject associatedThrowable;

    public VMPanic(final String message, @Nullable VMObject associatedThrowable)
    {
        super(message);
        this.associatedThrowable = associatedThrowable;
    }

    public VMPanic(final String message, final Throwable cause, @Nullable VMObject associatedThrowable)
    {
        super(message, cause);
        this.associatedThrowable = associatedThrowable;
    }

    public VMPanic(final Throwable cause, @Nullable VMObject associatedThrowable)
    {
        super(cause);
        this.associatedThrowable = associatedThrowable;
    }

    public VMPanic(final String message)
    {
        this(message, (VMObject) null);
    }

    public VMPanic(final String message, final Throwable cause)
    {
        this(message, cause, null);
    }

    public VMPanic(final Throwable cause)
    {
        this(cause, null);
    }


}
