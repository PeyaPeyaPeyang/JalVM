package tokyo.peya.langjal.vm.exceptions.invocation;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

public class NonStaticInvocationPanic extends IllegalInvocationTypePanic {
    public NonStaticInvocationPanic(@NotNull VMThread thread, @NotNull VMMethod method) {
        super(thread, method, "Cannot invoke non-static method '" + method.getName() + "' from static context.");
    }
}
