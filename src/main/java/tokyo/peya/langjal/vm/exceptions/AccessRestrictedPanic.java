package tokyo.peya.langjal.vm.exceptions;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.members.AccessibleObject;

@Getter
public class AccessRestrictedPanic extends VMPanic
{
    @Nullable
    private final AccessibleObject caller;
    @NotNull
    private final AccessibleObject target;

    public AccessRestrictedPanic(@Nullable AccessibleObject caller, @NotNull AccessibleObject target)
    {
        super("Access restricted!:" + (caller == null ? "J(al)VM": caller.getOwningClass()) + " -> " + target.getOwningClass());
        this.caller = caller;
        this.target = target;
    }
}
