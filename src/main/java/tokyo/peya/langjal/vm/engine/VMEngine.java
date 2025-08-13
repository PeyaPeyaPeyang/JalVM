package tokyo.peya.langjal.vm.engine;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.engine.threads.VMMainThread;
import tokyo.peya.langjal.vm.engine.threads.VMThread;

import java.util.ArrayList;
import java.util.List;

@Getter
public class VMEngine {
    private final JalVM vm;

    private final VMMainThread mainThread;
    @Getter(AccessLevel.NONE)
    private final List<VMThread> threads;

    private VMThread currentThread;

    public VMEngine(@NotNull JalVM vm) {
        this.vm = vm;
        this.mainThread = new VMMainThread(vm);
        this.threads = new ArrayList<>();

        this.threads.add(this.mainThread);
        this.currentThread = this.mainThread;
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
            this.currentThread = thread;
            if (thread.isAlive())
                thread.heartbeat();
            else {
                System.out.println("Thread " + thread.getName() + " is dead, marking for removal.");
                deadThreads.add(thread);
            }
        }

        this.currentThread = null;

        for (VMThread deadThread : deadThreads) {
            System.out.println("Dead thread wiped out: " + deadThread.getName());
            this.threads.remove(deadThread);
        }
    }

}
