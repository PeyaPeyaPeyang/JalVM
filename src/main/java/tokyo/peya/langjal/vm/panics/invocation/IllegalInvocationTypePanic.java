package tokyo.peya.langjal.vm.panics.invocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.values.VMObject;

public class IllegalInvocationTypePanic extends InvocationPanic
{
    public IllegalInvocationTypePanic(@NotNull VMThread thread, @NotNull VMMethod method, @NotNull String message,
                                      @Nullable VMObject associatedThrowable)
    {
        super(thread, method, message, associatedThrowable);
    }
    public IllegalInvocationTypePanic(@NotNull VMThread thread, @NotNull VMMethod method, @NotNull String message)
    {
        super(thread, method, message);
    }
}
