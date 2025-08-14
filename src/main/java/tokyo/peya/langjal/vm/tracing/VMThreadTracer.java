package tokyo.peya.langjal.vm.tracing;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VMThreadTracer {
    private final List<ThreadTracingEntry> history;

    public VMThreadTracer() {
        this.history = new ArrayList<>();
    }

    public void pushHistory(@NotNull ThreadTracingEntry entry) {
        this.history.add(entry);
    }

    public void pushHistory(@NotNull VMThreadTracer history) {
        this.history.addAll(history.history);
    }

    public List<ThreadTracingEntry> getHistory(@NotNull VMThread thread) {
        List<ThreadTracingEntry> threadHistory = new ArrayList<>();
        for (ThreadTracingEntry entry : this.history)
            if (entry.thread().equals(thread))
                threadHistory.add(entry);

        return Collections.unmodifiableList(threadHistory);
    }

    public List<ThreadTracingEntry> getHistory() {
        return Collections.unmodifiableList(this.history);
    }
}
