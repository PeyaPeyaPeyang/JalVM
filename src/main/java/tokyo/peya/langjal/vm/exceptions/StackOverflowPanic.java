package tokyo.peya.langjal.vm.exceptions;

public class StackOverflowPanic extends VMPanic {
    public StackOverflowPanic(final String message) {
        super(message);
    }

    public StackOverflowPanic(final String message, final Throwable cause) {
        super(message, cause);
    }

    public StackOverflowPanic(final Throwable cause) {
        super(cause);
    }
}
