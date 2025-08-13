package tokyo.peya.langjal.vm.exceptions;

public class LinkagePanic extends VMPanic {
    public LinkagePanic(String message) {
        super(message);
    }

    public LinkagePanic(String message, Throwable cause) {
        super(message, cause);
    }

    public LinkagePanic(Throwable cause) {
        super(cause);
    }
}
