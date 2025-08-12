package tokyo.peya.langjal.vm.engine;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.threads.VMMainThread;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

import java.util.ArrayList;
import java.util.List;

public class VMEngine {
    @Getter
    private final JalVM vm;

    @Getter
    private final VMMainThread mainThread;
    private final List<VMThread> threads;  // TODO: マルチスレッディング

    public VMEngine(@NotNull JalVM vm) {
        this.vm = vm;
        this.mainThread = new VMMainThread(vm);
        this.threads = new ArrayList<>();

        this.threads.add(this.mainThread);
    }

    public boolean isRunning() {
        return !this.threads.isEmpty();
    }

    public void startEngine() {
        while(this.isRunning())
            this.heartbeatThreads();
    }

    public void heartbeatThreads() {
        List<VMThread> deadThreads = new ArrayList<>();
        for (VMThread thread : this.threads) {
            if (thread.isAlive())
                thread.heartbeat();
            else {
                System.out.println("Thread " + thread.getThreadName() + " is dead, marking for removal.");
                deadThreads.add(thread);
            }
        }

        for (VMThread deadThread : deadThreads) {
            System.out.println("Dead thread wiped out: " + deadThread.getThreadName());
            this.threads.remove(deadThread);
        }
    }

}
