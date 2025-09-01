package tokyo.peya.langjal.vm.engine.threading;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VMThreadState
{
    NEW(0x0001),
    RUNNABLE(0x0004),
    BLOCKED_ON_MONITOR_ENTER(0x0400),
    WAITING_INDEFINITELY(0x0010),
    WAITING_WITH_TIMEOUT(0x0020),

    TERMINATED(0x0002);

    private final int mask;
}
