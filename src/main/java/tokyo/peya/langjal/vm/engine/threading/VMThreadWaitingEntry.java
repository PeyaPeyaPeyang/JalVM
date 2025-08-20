package tokyo.peya.langjal.vm.engine.threading;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

public record VMThreadWaitingEntry(
        @NotNull VMThread thread,
        @NotNull Mode mode,
        @NotNull Instant waitStarted,
        @NotNull Duration waitDuration
)
{
    public static VMThreadWaitingEntry acquire(@NotNull VMThread thread)
    {
        return new VMThreadWaitingEntry(
                thread,
                Mode.ACQUIRE,
                Instant.now(),
                Duration.ZERO
        );
    }

    public static VMThreadWaitingEntry waitTimed(@NotNull VMThread thread, @NotNull Duration waitDuration)
    {
        return new VMThreadWaitingEntry(
                thread,
                Mode.WAIT_TIMED,
                Instant.now(),
                waitDuration
        );
    }

    public static VMThreadWaitingEntry wait(@NotNull VMThread thread)
    {
        return new VMThreadWaitingEntry(
                thread,
                Mode.WAIT,
                Instant.now(),
                Duration.ZERO
        );
    }


    public enum Mode
    {
        WAIT,
        WAIT_TIMED,
        ACQUIRE
    }
}
