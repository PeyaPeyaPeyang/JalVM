package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.ArrayList;
import java.util.List;

public class VMHeap {
    private final List<VMClass> loadedClasses;

    public VMHeap() {
        this.loadedClasses = new ArrayList<>();
    }

    public void addClass(VMClass vmClass) {
        this.loadedClasses.add(vmClass);
    }

    public void removeClass(@NotNull String name) {
        loadedClasses.removeIf(vmClass -> vmClass.getReference().isEqualClass(name));
    }

    @Nullable
    public VMClass getLoadedClass(@NotNull String className) {
        for (VMClass vmClass : loadedClasses)
            if (vmClass.getReference().isEqualClass(className))
                return vmClass;

        return null;
    }

    @Nullable
    public VMClass getLoadedClass(@NotNull ClassReference className) {
        for (VMClass vmClass : loadedClasses)
            if (vmClass.getReference().equals(className))
                return vmClass;

        return null;
    }

    /* non-public */ List<VMClass> getLoadedClasses() {
        return this.loadedClasses;
    }
}
