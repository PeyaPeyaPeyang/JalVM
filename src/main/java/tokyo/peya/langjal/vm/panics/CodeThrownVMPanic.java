package tokyo.peya.langjal.vm.panics;

import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.values.VMObject;

public class CodeThrownVMPanic extends VMPanic
{
    public CodeThrownVMPanic(@Nullable VMObject associatedThrowable)
    {
        super("A throwable was thrown from the code.", associatedThrowable);
    }
}
