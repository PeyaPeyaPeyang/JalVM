package tokyo.peya.langjal.vm;

import com.sun.jdi.ClassLoaderReference;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMEngine;
import tokyo.peya.langjal.vm.engine.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;

@Getter
public class JalVM {
    private final VMHeap heap;

    private final VMEngine engine;

    public JalVM() {
        this.heap = new VMHeap();
        this.engine = new VMEngine(this);
    }

    public void execMain(@NotNull ClassReference clazz, @NotNull String[] args)
    {
        VMClass vmClass = this.heap.findClass(clazz);
        if (vmClass == null)
            throw new IllegalStateException("Unable to load class: " + clazz.getFullQualifiedName()
                    + ", please define it with VMHeap#defineClass() first!");

        this.execMain(vmClass, args);
    }

    public void execMain(@NotNull VMClass clazz, @NotNull String[] args)
    {
        VMMethod mainMethod = clazz.findMainMethod();
        if (mainMethod == null)
            throw new IllegalStateException("There is no main method in class:  "
                    + clazz.getReference().getFullQualifiedName());

        this.engine.executeEntryPointMethod(mainMethod);
    }
}
