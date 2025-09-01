package tokyo.peya.langjal.vm.panics;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.members.AccessibleObject;
import tokyo.peya.langjal.vm.values.VMObject;

@Getter
public class AccessRestrictedPanic extends VMPanic
{
    @Nullable
    private final AccessibleObject caller;
    @NotNull
    private final AccessibleObject target;

    public AccessRestrictedPanic(@Nullable AccessibleObject caller, @NotNull AccessibleObject target, @Nullable VMObject associatedThrowable)
    {
        super("Access restricted!:" + (caller == null ? "J(al)VM": caller.getOwningClass()) + " -> " + target.getOwningClass(),
              associatedThrowable
        );
        this.caller = caller;
        this.target = target;
    }

    public AccessRestrictedPanic(@Nullable AccessibleObject caller, @NotNull AccessibleObject target)
    {
        this(caller, target, null);
    }
}
