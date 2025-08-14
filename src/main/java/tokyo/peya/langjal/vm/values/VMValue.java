package tokyo.peya.langjal.vm.values;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.tracing.VMValueTracer;

public interface VMValue {
    @NotNull
    VMType getType();

    boolean isCompatibleTo(@NotNull VMValue other);

    default boolean isCategory2() {
        return this.getType().getType().getCategory() == 2;
    }

    String toString();
}
