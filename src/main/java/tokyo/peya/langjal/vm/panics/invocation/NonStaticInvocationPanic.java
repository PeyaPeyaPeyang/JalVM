package tokyo.peya.langjal.vm.panics.invocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.values.VMObject;

public class NonStaticInvocationPanic extends IllegalInvocationTypePanic
{
    public NonStaticInvocationPanic(@NotNull VMThread thread, @NotNull VMMethod method, @Nullable VMObject associatedThrowable)
    {
        super(thread, method, "Cannot invoke non-static method '" + method.getName() + "' from static context.", associatedThrowable);
    }
    public NonStaticInvocationPanic(@NotNull VMThread thread, @NotNull VMMethod method)
    {
        super(thread, method, "Cannot invoke non-static method '" + method.getName() + "' from static context.");
    }
}
