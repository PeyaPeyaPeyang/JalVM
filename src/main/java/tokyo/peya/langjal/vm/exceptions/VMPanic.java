package tokyo.peya.langjal.vm.exceptions;

public class VMPanic extends RuntimeException {
    public VMPanic(final String message) {
        super(message);
    }

    public VMPanic(final String message, final Throwable cause) {
        super(message, cause);
    }

    public VMPanic(final Throwable cause) {
        super(cause);
    }
}
