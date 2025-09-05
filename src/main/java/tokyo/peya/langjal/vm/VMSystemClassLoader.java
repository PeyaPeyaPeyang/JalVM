package tokyo.peya.langjal.vm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.vm.api.events.VMDefineClassEvent;
import tokyo.peya.langjal.vm.api.events.VMLinkClassEvent;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.injections.InjectorManager;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VMSystemClassLoader implements VMComponent
{
    private final JalVM vm;
    private final VMHeap heap;
    private final InjectorManager injector;

    private boolean isLinking;
    private final Deque<VMType<?>> linkingQueue;

    private final ExecutorService executor;
    private final Map<ClassReference, CompletableFuture<VMClass>> defining;


    public VMSystemClassLoader(@NotNull JalVM vm, @NotNull VMHeap heap)
    {
        this.vm = vm;
        this.heap = heap;
        this.injector = new InjectorManager();
        this.linkingQueue = new ConcurrentLinkedDeque<>();

        this.executor = Executors.newWorkStealingPool();
        this.defining = new ConcurrentHashMap<>();
    }

    @Nullable
    public VMClass findClassSafe(@NotNull ClassReference ref)
    {
        // すでにロード済みなら即返す
        VMClass vmClass = this.heap.getLoadedClass(ref);
        if (vmClass != null)
            return vmClass;

        CompletableFuture<VMClass> definingFuture = this.createDefinitionTask(ref);
        return this.waitForDefinition(definingFuture);
    }

    private CompletableFuture<VMClass> createDefinitionTask(@NotNull ClassReference ref)
    {
        return this.defining.computeIfAbsent(ref, r ->
                CompletableFuture.supplyAsync(() -> {
                    // defineClass 内でも既にロード済みかチェック
                    VMClass existing = this.heap.getLoadedClass(r);
                    if (existing != null)
                        return existing;

                    try
                    {
                        return this.defineClass(r);
                    }
                    finally
                    {
                        this.defining.remove(r);
                    }
                }, this.executor)
        );
    }

    private VMClass waitForDefinition(@NotNull CompletableFuture<? extends VMClass> future)
    {
        try
        {
            return future.get();
        }
        catch (Exception e)
        {
            throw new VMPanic("Failed to define class: " + e.getMessage(), e);
        }
    }

    private VMClass defineClass(@NotNull ClassReference ref)
    {
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

        this.defineDependingClasses(classNode);
        this.linkType(vmClass);

        return vmClass;
    }

    private void defineDependingClasses(@NotNull ClassNode cn)
    {
        String superName = cn.superName;
        if (superName != null)
            this.createDefinitionTask(ClassReference.of(superName));

        for (String iface : cn.interfaces)
            this.createDefinitionTask(ClassReference.of(iface));

        for (FieldNode field : cn.fields)
            this.defineDependingClass(Type.getType(field.desc));

        for (MethodNode method : cn.methods)
        {
            Type methodType = Type.getMethodType(method.desc);
            Type returnType = methodType.getReturnType();

            this.defineDependingClass(returnType);

            for (Type argType : methodType.getArgumentTypes())
                this.defineDependingClass(argType);

            for (String ex : method.exceptions)
                this.createDefinitionTask(ClassReference.of(ex));
        }
    }

    private void defineDependingClass(@NotNull Type type)
    {
        if (type.getSort() == Type.OBJECT)
            this.createDefinitionTask(ClassReference.of(type.getClassName()));
        else if (type.getSort() == Type.ARRAY)
            if (type.getElementType().getSort() == Type.OBJECT)
                this.createDefinitionTask(ClassReference.of(type.getElementType().getClassName()));
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
