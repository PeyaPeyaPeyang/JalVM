package tokyo.peya.langjal.vm.engine.threading;

public enum VMThreadState
{
    NEW,
    RUNNABLE,
    BLOCKED,
    WAITING,
    TIMED_WAITING,

    TERMINATED;
}
