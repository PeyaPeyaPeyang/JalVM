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
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.exceptions.AccessRestrictedPanic;
import tokyo.peya.langjal.vm.exceptions.invocation.NonStaticInvocationPanic;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

import java.util.Arrays;

@Getter
public class VMMethod implements RestrictedAccessor
{
    private final VMClass clazz;
    private final MethodNode methodNode;

    private final AccessLevel accessLevel;
    private final AccessAttributeSet accessAttributes;

    private final MethodDescriptor descriptor;
    private final VMType<?> returnType;
    private final VMType<?>[] parameterTypes;

    public VMMethod(@NotNull VMClass clazz, @NotNull MethodNode methodNode)
    {
        this.clazz = clazz;
        this.methodNode = methodNode;

        this.accessLevel = AccessLevel.fromAccess(methodNode.access);
        this.accessAttributes = AccessAttributeSet.fromAccess(methodNode.access);

        this.descriptor = MethodDescriptor.parse(methodNode.desc);
        this.returnType = new VMType<>(this.descriptor.getReturnType());
        this.parameterTypes = Arrays.stream(this.descriptor.getParameterTypes())
                                    .map(VMType::new)
                                    .toArray(VMType[]::new);
    }

    public VMInterpreter createInterpreter(
            @NotNull JalVM vm)
    {
        return new BytecodeInterpreter(vm, this.methodNode);
        // return new DebugInterpreter(vm, engine, frame);
    }

    public void linkTypes(@NotNull VMSystemClassLoader cl)
    {
        this.returnType.linkClass(cl);
        for (VMType<?> type : this.parameterTypes)
            type.linkClass(cl);
    }

    public void invokeStatic(@Nullable MethodInsnNode operand, @NotNull VMThread thread, @Nullable VMClass caller, boolean isVMDecree,
                             @NotNull VMValue... args)
    {
        if (!this.accessAttributes.has(AccessAttribute.STATIC))
            throw new NonStaticInvocationPanic(thread, this);
        else if (!this.canAccessFrom(caller))
            throw new AccessRestrictedPanic(caller, this);

        thread.invokeMethod(this, isVMDecree, args);
    }

    public void invokeBypassAccess(@NotNull VMThread thread, @Nullable VMClass caller, @NotNull VMValue... args)
    {
        if (!this.accessAttributes.has(AccessAttribute.STATIC))
            throw new NonStaticInvocationPanic(thread, this);

        thread.createInterrupting(this, (_) -> {}, args);
    }

    public void invokeInstanceMethod(@Nullable MethodInsnNode operand, @NotNull VMThread thread, @Nullable VMClass caller,
                                     @NotNull VMObject instance, boolean isVMDecree, @NotNull VMValue... args)
    {
        if (this.accessAttributes.has(AccessAttribute.STATIC))
            throw new NonStaticInvocationPanic(thread, this);
        else if (!this.canAccessFrom(caller))
            throw new AccessRestrictedPanic(caller, this);

        VMValue[] newArgs = new VMValue[args.length + 1];
        newArgs[0] = instance; // インスタンスを最初の引数に
        System.arraycopy(args, 0, newArgs, 1, args.length);

        thread.invokeMethod(this, isVMDecree, newArgs);
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
        return this.clazz.getReference() + "." + this.methodNode.name + this.methodNode.desc +
                " (access: " + this.accessLevel + ", attributes: " + this.accessAttributes + ")";
    }
}
