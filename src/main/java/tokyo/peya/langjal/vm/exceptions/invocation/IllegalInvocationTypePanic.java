package tokyo.peya.langjal.vm.exceptions.invocation;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

public class IllegalInvocationTypePanic extends InvocationPanic
{
    public IllegalInvocationTypePanic(@NotNull VMThread thread, @NotNull VMMethod method, @NotNull String message)
    {
        super(thread, method, message);
    }
}
