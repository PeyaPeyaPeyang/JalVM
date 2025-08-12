package tokyo.peya.langjal.vm.exceptions;

public class IllegalOperandPanic extends VMPanic{
    public IllegalOperandPanic(final String message) {
        super(message);
    }

    public IllegalOperandPanic(final String message, final Throwable cause) {
        super(message, cause);
    }

    public IllegalOperandPanic(final Throwable cause) {
        super(cause);
    }
}
