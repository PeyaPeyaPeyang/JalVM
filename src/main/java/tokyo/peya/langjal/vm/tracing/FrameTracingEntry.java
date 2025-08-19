package tokyo.peya.langjal.vm.tracing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.engine.VMFrame;

public record FrameTracingEntry(
        @NotNull
        FrameManipulationType type,
        @NotNull
        VMFrame frame,
        @Nullable
        AbstractInsnNode performer,

        int jumpTargetInstructionIndex
)
{
    public static FrameTracingEntry frameIn(@NotNull VMFrame frame) {
        return new FrameTracingEntry(
                FrameManipulationType.FRAME_IN,
                frame,
                null,
                -1
        );
    }
    public static FrameTracingEntry frameOut(@NotNull VMFrame frame) {
        return new FrameTracingEntry(
                FrameManipulationType.FRAME_IN,
                frame,
                null,
                -1
        );
    }

    public static FrameTracingEntry insideJump(@NotNull VMFrame frame, @NotNull AbstractInsnNode performer, int jumpTargetInstructionIndex) {
        return new FrameTracingEntry(
                FrameManipulationType.FRAME_EXECUTION_JUMP,
                frame,
                performer,
                jumpTargetInstructionIndex
        );
    }
}
