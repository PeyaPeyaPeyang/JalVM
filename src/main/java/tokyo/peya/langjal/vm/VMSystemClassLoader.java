package tokyo.peya.langjal.vm;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import tokyo.peya.langjal.vm.api.events.VMDefineClassEvent;
import tokyo.peya.langjal.vm.api.events.VMLinkClassEvent;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.injections.InjectorManager;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;

import java.util.ArrayDeque;
import java.util.Deque;

public class VMSystemClassLoader implements VMComponent
{
    private final JalVM vm;
    private final VMHeap heap;
    private final InjectorManager injector;

    private boolean isLinking;
    private final Deque<VMType<?>> linkingQueue;

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
        if (this.heap.getLoadedClass(ClassReference.of(name)) != null)
            throw new IllegalStateException("Class " + name + " is already defined!");

        ClassReference ref = ClassReference.of(name);
        System.out.println("Defining class: " + ref.getFullQualifiedName());
        VMDefineClassEvent event = new VMDefineClassEvent(this.vm, ref, classNode);
        this.vm.getEventManager().dispatchEvent(event);

        VMClass vmClass = new VMClass(this.vm, classNode);
        this.heap.addClass(vmClass);

        this.linkType(vmClass);

        return vmClass;
    }

    public void linkType(@NotNull VMType<?> vmClass)
    {
        if (vmClass.isLinked())
            return;

        this.linkLater(vmClass);
        this.resumeLinking();
    }
    public void linkLater(@NotNull VMType<?> vmClass)
    {
        if (vmClass.isLinked())
            return;

        this.linkingQueue.add(vmClass);
    }

    public void resumeLinking()
    {
        if (this.isLinking)
            return;

        this.isLinking = true;
        while (!this.linkingQueue.isEmpty())
        {
            VMType<?> vmClass = this.linkingQueue.pollFirst();
            if (vmClass.isLinked())
                continue;

            this.vm.getEventManager().dispatchEvent(new VMLinkClassEvent(this.vm, vmClass));
            vmClass.link(this.vm);
            // Array などを弾く
            if (VMClass.class == vmClass.getClass())
            {
                VMClass clazz = (VMClass) vmClass;
                this.injector.injectClass(this, clazz);
            }
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

    @Override
    public @NotNull JalVM getVM()
    {
        return this.vm;
    }
}
