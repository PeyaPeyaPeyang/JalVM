package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.ArrayList;
import java.util.List;

public class VMHeap {
    private final List<VMClass> loadedClass;

    public VMHeap() {
        this.loadedClass = new ArrayList<>();
    }

    public void addClass(VMClass vmClass) {
        this.loadedClass.add(vmClass);
    }

    public void removeClass(@NotNull String name) {
        loadedClass.removeIf(vmClass -> vmClass.getReference().isEqualClass(name));
    }

    @Nullable
    public VMClass findClass(@NotNull String className) {
        for (VMClass vmClass : loadedClass)
            if (vmClass.getReference().isEqualClass(className))
                return vmClass;

        return null;
    }

    @Nullable
    public VMClass findClass(@NotNull ClassReference className) {
        for (VMClass vmClass : loadedClass)
            if (vmClass.getReference().equals(className))
                return vmClass;

        return null;
    }

    @NotNull
    public VMClass defineClass(@NotNull ClassNode classNode)
    {
        String name = classNode.name;
        if (this.findClass(name) != null)
            throw new IllegalStateException("Class " + name + " is already defined!");

        VMClass vmClass = new VMClass(classNode);
        this.addClass(vmClass);

        return vmClass;
    }

    @NotNull
    public VMClass defineClass(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        try {
            reader.accept(classNode, 0);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Failed to read class bytes: " + e.getMessage(), e);
        }

        return this.defineClass(classNode);
    }
}
