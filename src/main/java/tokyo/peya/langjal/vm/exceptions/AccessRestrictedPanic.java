package tokyo.peya.langjal.vm.exceptions;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.members.RestrictedAccessor;

@Getter
public class AccessRestrictedPanic extends VMPanic {
    @Nullable
    private final RestrictedAccessor caller;
    @NotNull
    private final RestrictedAccessor target;

    public AccessRestrictedPanic(@Nullable RestrictedAccessor caller, @NotNull RestrictedAccessor target) {
        super("Access restricted!:" + (caller == null ? "J(al)VM": caller.getOwningClass()) + " -> " + target.getOwningClass());
        this.caller = caller;
        this.target = target;
    }
}
