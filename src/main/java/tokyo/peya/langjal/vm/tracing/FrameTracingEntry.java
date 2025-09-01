package tokyo.peya.langjal.vm.tracing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import tokyo.peya.langjal.vm.engine.ExceptionHandlerDirective;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.panics.VMPanic;

public record FrameTracingEntry(
        @NotNull
        FrameManipulationType type,
        @NotNull
        VMFrame frame,
        @Nullable
        AbstractInsnNode performer,

        @Nullable
        VMPanic panic,
        int jumpTargetInstructionIndex
)
{
    public static FrameTracingEntry frameIn(@NotNull VMFrame frame) {
        return new FrameTracingEntry(
                FrameManipulationType.FRAME_IN,
                frame,
                null,
                null,
                -1
        );
    }
    public static FrameTracingEntry frameOut(@NotNull VMFrame frame) {
        return new FrameTracingEntry(
                FrameManipulationType.FRAME_OUT,
                frame,
                null,
                null,
                -1
        );
    }

    public static FrameTracingEntry insideJump(@NotNull VMFrame frame, @NotNull AbstractInsnNode performer, int jumpTargetInstructionIndex) {
        return new FrameTracingEntry(
                FrameManipulationType.FRAME_EXECUTION_JUMP,
                frame,
                performer,
                null,
                jumpTargetInstructionIndex
        );
    }

    public static FrameTracingEntry exceptionThrown(@NotNull VMFrame vmFrame, @NotNull VMPanic panic, @Nullable ExceptionHandlerDirective handler)
    {
        return new FrameTracingEntry(
                FrameManipulationType.EXCEPTION_THROWN,
                vmFrame,
                null,
                panic,
                handler == null ? -1 : handler.startInstructionIndex()
        );
    }
}
