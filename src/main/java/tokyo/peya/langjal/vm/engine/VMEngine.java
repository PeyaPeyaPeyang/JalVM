package tokyo.peya.langjal.vm.engine;

import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.ArrayList;
import java.util.List;

public class VMEngine {
    private final JalVM vm;

    private final VMMainThread mainThread;
    private final List<VMThread> threads;  // TODO: マルチスレッディング

    public VMEngine(@NotNull JalVM vm) {
        this.vm = vm;
        this.mainThread = new VMMainThread(vm);
        this.threads = new ArrayList<>();

        this.threads.add(this.mainThread);
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

    public void executeMain(@NotNull ClassReference clazz, @NotNull String[] args) {
        VMClass vmClass = this.vm.getHeap().getLoadedClass(clazz);
        if (vmClass == null)
            throw new IllegalStateException("Unable to load class: " + clazz.getFullQualifiedName()
                    + ", please define it with VMHeap#defineClass() first!");

        this.executeMain(vmClass, args);
    }

    public void executeMain(@NotNull VMClass clazz, @NotNull String[] args) {
        VMMethod mainMethod = clazz.findMainMethod();
        if (mainMethod == null)
            throw new IllegalStateException("There is no main method in class:  "
                    + clazz.getReference().getFullQualifiedName());

        this.mainThread.executeEntryPointMethod(mainMethod);
    }
}
