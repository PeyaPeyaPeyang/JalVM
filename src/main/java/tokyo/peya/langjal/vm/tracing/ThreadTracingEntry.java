package tokyo.peya.langjal.vm.tracing;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.threading.VMThread;

public record ThreadTracingEntry(
        @NotNull
        ThreadManipulationType type,
        @NotNull
        VMThread thread
)
{
}
