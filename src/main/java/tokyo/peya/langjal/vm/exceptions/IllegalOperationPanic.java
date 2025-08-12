package tokyo.peya.langjal.vm.exceptions;

public class IllegalOperationPanic extends VMPanic {
    public IllegalOperationPanic(final String message) {
        super(message);
    }

    public IllegalOperationPanic(final String message, final Throwable cause) {
        super(message, cause);
    }

    public IllegalOperationPanic(final Throwable cause) {
        super(cause);
    }
}
