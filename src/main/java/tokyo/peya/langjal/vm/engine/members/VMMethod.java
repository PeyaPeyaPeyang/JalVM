package tokyo.peya.langjal.vm.engine.members;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.AccessAttributeSet;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMInterpreter;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.BytecodeInterpreter;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.AccessRestrictedPanic;
import tokyo.peya.langjal.vm.exceptions.invocation.NonStaticInvocationPanic;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMConstructorObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMMethodObject;

import java.util.Arrays;

@Getter
public class VMMethod implements AccessibleObject
{
    private final JalVM vm;
    private final VMClass clazz;
    private final int slot;
    private final MethodNode methodNode;

    private final AccessLevel accessLevel;
    private final AccessAttributeSet accessAttributes;

    private final MethodDescriptor descriptor;
    private final VMType<?> returnType;
    private final VMType<?>[] parameterTypes;

    @Getter(lombok.AccessLevel.NONE)
    private VMMethodObject methodObject;

    public VMMethod(@NotNull JalVM vm, @NotNull VMClass clazz, int slot, @NotNull MethodNode methodNode)
    {
        this.vm = vm;
        this.clazz = clazz;
        this.slot = slot;
        this.methodNode = methodNode;

        this.accessLevel = AccessLevel.fromAccess(methodNode.access);
        this.accessAttributes = AccessAttributeSet.fromAccess(methodNode.access);

        this.descriptor = MethodDescriptor.parse(methodNode.desc);
        this.returnType = VMType.of(vm, this.descriptor.getReturnType());
        this.parameterTypes = Arrays.stream(this.descriptor.getParameterTypes())
                                    .map(m -> VMType.of(vm, m))
                                    .toArray(VMType[]::new);

    }

    public VMMethodObject getMethodObject()
    {
        if (this.methodNode != null)
        {
            if (this.isConstructor())
                this.methodObject = new VMConstructorObject(this.vm, this);
            else
                this.methodObject = new VMMethodObject(this.vm, this);
        }

        return this.methodObject;
    }

    public VMInterpreter createInterpreter(
            @NotNull JalVM vm)
    {
        return new BytecodeInterpreter(vm, this.methodNode);
        // return new DebugInterpreter(vm, engine, frame);
    }

    public void invokeStatic(@Nullable MethodInsnNode operand, @NotNull VMThread thread, @Nullable VMClass caller, boolean isVMDecree,
                             @NotNull VMValue... args)
    {
        if (!this.accessAttributes.has(AccessAttribute.STATIC))
            throw new NonStaticInvocationPanic(thread, this);
        else if (!this.canAccessFrom(caller))
            throw new AccessRestrictedPanic(caller, this);

        thread.invokeMethod(this, isVMDecree, null,args);
    }

    public void invokeBypassAccess(@NotNull VMThread thread, @Nullable VMClass caller, @NotNull VMValue... args)
    {
        if (!this.accessAttributes.has(AccessAttribute.STATIC))
            throw new NonStaticInvocationPanic(thread, this);

        thread.invokeInterrupting(this, (_) -> {}, args);
    }

    public void invokeInstanceMethod(@Nullable MethodInsnNode operand, @NotNull VMThread thread, @Nullable VMClass caller,
                                     @NotNull VMObject instance, boolean isVMDecree, @NotNull VMValue... args)
    {
        if (this.accessAttributes.has(AccessAttribute.STATIC))
            throw new NonStaticInvocationPanic(thread, this);
        else if (!this.canAccessFrom(caller))
            throw new AccessRestrictedPanic(caller, this);

        thread.invokeMethod(this, isVMDecree, instance, args);
    }

    @NotNull
    public String getName()
    {
        return this.methodNode.name;
    }

    public int getMaxStackSize()
    {
        return this.methodNode.maxStack;
    }

    public int getMaxLocals()
    {
        return this.methodNode.maxLocals;
    }

    @Override
    public VMClass getOwningClass()
    {
        return this.clazz;
    }

    @Override
    public String toString()
    {
        return this.clazz.getReference() + "->" + this.methodNode.name + this.methodNode.desc +
                " (access: " + this.accessLevel + ", attributes: " + this.accessAttributes + ")";
    }

    public boolean isConstructor()
    {
        return this.methodNode.name.equals("<init>") && !this.accessAttributes.has(AccessAttribute.STATIC);
    }
}
