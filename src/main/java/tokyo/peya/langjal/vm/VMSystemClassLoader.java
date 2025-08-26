package tokyo.peya.langjal.vm;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import tokyo.peya.langjal.vm.api.events.VMDefineClassEvent;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.injections.InjectorManager;
import tokyo.peya.langjal.vm.references.ClassReference;

import java.util.ArrayDeque;
import java.util.Deque;

public class VMSystemClassLoader
{
    @Getter
    private final JalVM vm;
    private final VMHeap heap;
    private final InjectorManager injector;

    private boolean isLinking;
    private final Deque<VMClass> linkingQueue;

    public VMSystemClassLoader(@NotNull JalVM vm, @NotNull VMHeap heap)
    {
        this.vm = vm;
        this.heap = heap;
        this.injector = new InjectorManager();
        this.linkingQueue = new ArrayDeque<>();
    }

    @Nullable
    public VMClass findClassSafe(@NotNull ClassReference ref)
    {
        VMClass vmClass = this.heap.getLoadedClass(ref);
        if (vmClass != null)
            return vmClass;

        byte[] classBytes = this.vm.getClassPaths().findClassBytes(ref);
        if (classBytes == null)
            return null;

        return this.defineClass(classBytes);
    }

    @NotNull
    public VMClass findClass(@NotNull ClassReference ref)
    {
        VMClass vmClass = this.findClassSafe(ref);
        if (vmClass == null)
            throw new IllegalArgumentException("No class found for: " + ref.getFullQualifiedName());

        return vmClass;
    }

    @NotNull
    public VMClass defineClass(@NotNull ClassNode classNode)
    {
        String name = classNode.name;
        if (this.heap.getLoadedClass(name) != null)
            throw new IllegalStateException("Class " + name + " is already defined!");

        ClassReference ref = ClassReference.of(name);
        VMDefineClassEvent event = new VMDefineClassEvent(this.vm, ref, classNode);
        this.vm.getEventManager().dispatchEvent(event);

        VMClass vmClass = new VMClass(this, classNode);
        this.heap.addClass(vmClass);

        this.linkingQueue.add(vmClass);
        this.resumeLinking();

        return vmClass;
    }

    public void resumeLinking()
    {
        if (this.isLinking || !this.vm.isRunning())
            return;

        this.isLinking = true;
        while (!this.linkingQueue.isEmpty())
        {
            VMClass vmClass = this.linkingQueue.pollFirst();
            vmClass.link(this);
            // クラスにネイティブ等を注入
            this.injector.injectClass(this, vmClass);
        }
        this.isLinking = false;
    }

    @NotNull
    public VMClass defineClass(byte[] classBytes)
    {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        try
        {
            reader.accept(classNode, 0);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Failed to read class bytes: " + e.getMessage(), e);
        }

        return this.defineClass(classNode);
    }
}
