package tokyo.peya.langjal.vm.tracing;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.VMFrame;

public record FrameTracingEntry(
        @NotNull
        FrameManipulationType type,
        @NotNull
        VMFrame frame
) {
}
