package tokyo.peya.langjal.vm.exceptions.invocation;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;

public class InvocationPanic extends VMPanic
{
    @NotNull
    private final VMThread thread;
    @NotNull
    private final VMMethod method;

    public InvocationPanic(@NotNull VMThread thread, @NotNull VMMethod method, @NotNull String message)
    {
        super(message);
        this.thread = thread;
        this.method = method;
    }
}
