package tokyo.peya.langjal.vm.exceptions;

public class UnknownInstructionPanic extends VMPanic
{
    public UnknownInstructionPanic(final String message)
    {
        super(message);
    }

    public UnknownInstructionPanic(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public UnknownInstructionPanic(final Throwable cause)
    {
        super(cause);
    }
}
