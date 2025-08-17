package tokyo.peya.langjal.vm.exceptions;

public class NoReferencePanic extends VMPanic
{
    public NoReferencePanic(final String message)
    {
        super(message);
    }

    public NoReferencePanic(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public NoReferencePanic(final Throwable cause)
    {
        super(cause);
    }
}
