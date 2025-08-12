package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;

public class VMClassLoader {
    private final VMHeap heap;

    public VMClassLoader(@NotNull VMHeap heap) {
        this.heap = heap;
    }

    @Nullable
    public VMClass findClassSafe(@NotNull ClassReference ref) {
        VMClass vmClass = this.heap.getLoadedClass(ref);
        return vmClass;

        // TODO: クラスパスから読み込む
    }

    @NotNull
    public VMClass findClass(@NotNull ClassReference ref) {
        VMClass vmClass = this.findClassSafe(ref);
        if (vmClass == null)
            throw new IllegalArgumentException("No class found for: " + ref.getFullQualifiedName());

        return vmClass;
    }

    @NotNull
    public VMClass defineClass(@NotNull ClassNode classNode) {
        String name = classNode.name;
        if (this.heap.getLoadedClass(name) != null)
            throw new IllegalStateException("Class " + name + " is already defined!");

        VMClass vmClass = new VMClass(classNode);
        this.heap.addClass(vmClass);
        vmClass.linkMembers(this);

        return vmClass;
    }

    @NotNull
    public VMClass defineClass(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        try {
            reader.accept(classNode, 0);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read class bytes: " + e.getMessage(), e);
        }

        return this.defineClass(classNode);
    }
}
