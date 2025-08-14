package tokyo.peya.langjal.vm.tracing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

public record FrameTracingEntry(
        @NotNull
        FrameManipulationType type,
        @NotNull
        VMFrame frame
) {
}
