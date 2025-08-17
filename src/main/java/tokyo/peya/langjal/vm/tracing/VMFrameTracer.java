package tokyo.peya.langjal.vm.tracing;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VMFrameTracer
{
    private final List<FrameTracingEntry> history;

    public VMFrameTracer()
    {
        this.history = new ArrayList<>();
    }

    public void pushHistory(@NotNull FrameTracingEntry entry)
    {
        this.history.add(entry);
    }

    public void pushHistory(@NotNull VMFrameTracer history)
    {
        this.history.addAll(history.history);
    }

    public List<FrameTracingEntry> getHistory()
    {
        return Collections.unmodifiableList(this.history);
    }
}
