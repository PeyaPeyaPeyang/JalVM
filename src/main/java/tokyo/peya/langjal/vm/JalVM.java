package tokyo.peya.langjal.vm;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMEngine;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;

@Getter
public class JalVM {
    private final VMHeap heap;
    private final ClassPaths classPaths;
    private final VMClassLoader classLoader;
    private final VMEngine engine;

    public JalVM() {
        System.out.println("Initialising J(al)VM...");

        this.heap = new VMHeap();
        this.classPaths = new ClassPaths();
        this.classLoader = new VMClassLoader(this, this.heap);
        this.engine = new VMEngine(this);
    }

    public void startJVM() {
        System.out.println("Starting J(al)VM...");
        this.engine.startEngine();
    }

    public void executeMain(@NotNull ClassReference clazz, @NotNull String[] args) {
        VMClass vmClass = this.heap.getLoadedClass(clazz);
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

        this.engine.getMainThread().executeEntryPointMethod(mainMethod);

        this.startJVM();
    }
}
