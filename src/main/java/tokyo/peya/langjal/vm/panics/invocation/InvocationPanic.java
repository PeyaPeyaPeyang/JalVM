package tokyo.peya.langjal.vm.panics.invocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.values.VMObject;

public class InvocationPanic extends VMPanic
{
    @NotNull
    private final VMThread thread;
    @NotNull
    private final VMMethod method;

    public InvocationPanic(@NotNull VMThread thread, @NotNull VMMethod method, @NotNull String message)
    {
        this(thread, method, message, null);
    }

    public InvocationPanic(@NotNull VMThread thread, @NotNull VMMethod method, @NotNull String message,
                           @Nullable VMObject associatedThrowable)
    {
        super(message, associatedThrowable);
        this.thread = thread;
        this.method = method;
    }
}
