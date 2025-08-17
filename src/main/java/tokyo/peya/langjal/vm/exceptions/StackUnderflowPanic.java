package tokyo.peya.langjal.vm.exceptions;

public class StackUnderflowPanic extends VMPanic
{
    public StackUnderflowPanic(final String message)
    {
        super(message);
    }

    public StackUnderflowPanic(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public StackUnderflowPanic(final Throwable cause)
    {
        super(cause);
    }
}
