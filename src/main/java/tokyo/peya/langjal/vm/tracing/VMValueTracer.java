package tokyo.peya.langjal.vm.tracing;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VMValueTracer
{
    private final List<ValueTracingEntry> history;

    public VMValueTracer()
    {
        this.history = new ArrayList<>();
    }

    public void pushHistory(@NotNull ValueTracingEntry entry)
    {
        this.history.add(entry);
    }

    public void pushHistory(@NotNull VMValueTracer history)
    {
        this.history.addAll(history.history);
    }

    public List<ValueTracingEntry> getHistory(@NotNull VMValue value)
    {
        List<ValueTracingEntry> result = new ArrayList<>();
        for (ValueTracingEntry entry : this.history)
            if (entry.value().equals(value))
                result.add(entry);
            else if (entry.combinationValue() != null && entry.combinationValue().equals(value))
                result.add(entry);
            else if (entry.combinationValue2() != null && entry.combinationValue2().equals(value))
                result.add(entry);
        return Collections.unmodifiableList(result);
    }

    public List<ValueTracingEntry> getHistory()
    {
        return Collections.unmodifiableList(this.history);
    }
}
