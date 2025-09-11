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
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.VMInterpreter;
import tokyo.peya.langjal.vm.engine.BytecodeInterpreter;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.panics.AccessRestrictedPanic;
import tokyo.peya.langjal.vm.panics.invocation.NonStaticInvocationPanic;
import tokyo.peya.langjal.vm.values.VMAnnotation;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMConstructorObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMMethodObject;

import java.util.Arrays;
import java.util.List;

@Getter
public class VMMethod implements AccessibleObject
{
    private final JalVM vm;
    private final VMClass clazz;
    private final int slot;
    private final MethodNode methodNode;
    private final List<VMAnnotation> annotations;

    private final AccessLevel accessLevel;
    private final AccessAttributeSet accessAttributes;

    private final MethodDescriptor descriptor;
    private final VMType<?> returnType;
    private final VMType<?>[] parameterTypes;
    private final boolean isSignaturePolymorphic;

    @Getter(lombok.AccessLevel.NONE)
    private VMMethodObject methodObject;

    public VMMethod(@NotNull JalVM vm, @NotNull VMClass clazz, int slot, @NotNull MethodNode methodNode)
    {
        this.vm = vm;
        this.clazz = clazz;
        this.slot = slot;
        this.methodNode = methodNode;
        this.annotations = VMAnnotation.of(vm, methodNode.visibleAnnotations);

        this.accessLevel = AccessLevel.fromAccess(methodNode.access);
        this.accessAttributes = AccessAttributeSet.fromAccess(methodNode.access);

        this.descriptor = MethodDescriptor.parse(methodNode.desc);
        this.returnType = VMType.of(vm, this.descriptor.getReturnType());
        this.parameterTypes = Arrays.stream(this.descriptor.getParameterTypes())
                                    .map(m -> VMType.of(vm, m))
                                    .toArray(VMType[]::new);

        this.isSignaturePolymorphic =
                (clazz.getReference().isEqualClass("java/lang/invoke/MethodHandle")
                        || clazz.getReference().isEqualClass("java/lang/invoke/VarHandle")
                ) && this.parameterTypes.length == 1
                        && this.parameterTypes[0].equals(VMType.ofGenericArray(vm))
                        && this.accessAttributes.has(AccessAttribute.NATIVE)
                        && this.accessAttributes.has(AccessAttribute.VARARGS);
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

    public VMInterpreter createInterpreter()
    {
        return new BytecodeInterpreter(this.vm, this.methodNode);
    }

    public void invokeStatic(@Nullable MethodInsnNode operand, @NotNull VMFrame frame, @Nullable VMClass caller, boolean isVMDecree,
                             @NotNull VMValue... args)
    {
        if (!this.accessAttributes.has(AccessAttribute.STATIC))
            throw new NonStaticInvocationPanic(frame.getThread(), this);
        else if (!this.canAccessFrom(caller))
            throw new AccessRestrictedPanic(caller, this);

        frame.getThread().invokeMethod(this, isVMDecree, null,args);
    }

    public void invokeBypassAccess(@NotNull VMFrame frame, @Nullable VMClass caller, @NotNull VMValue... args)
    {
        if (!this.accessAttributes.has(AccessAttribute.STATIC))
            throw new NonStaticInvocationPanic(frame.getThread(), this);

        frame.getThread().invokeInterrupting(this, (_) -> {}, args);
    }

    public void invokeInstanceMethod(@Nullable MethodInsnNode operand, @NotNull VMFrame frame, @Nullable VMClass caller,
                                     @NotNull VMObject instance, boolean isVMDecree, @NotNull VMValue... args)
    {
        if (this.accessAttributes.has(AccessAttribute.STATIC))
            throw new NonStaticInvocationPanic(frame.getThread(), this);
        else if (!this.canAccessFrom(caller))
            throw new AccessRestrictedPanic(caller, this);

        frame.getThread().invokeMethod(this, isVMDecree, instance, args);
    }


    protected boolean checkOwnerSuitability(@Nullable VMClass owner)
    {
        return owner == null || owner.equals(this.getOwningClass());
    }

    protected boolean checkNameSuitability(@NotNull String methodName)
    {
        return this.getName().equals(methodName);
    }

    protected boolean checkReturnTypeSuitability(@Nullable VMType<?> returnType)
    {
        if (this.isSignaturePolymorphic) // シグネチャ多相メソッドは戻り値の型を問わない
            return true;

        return returnType == null || returnType.equals(this.getReturnType());
    }

    protected boolean checkAccessSuitability(@Nullable VMClass caller)
    {
        return this.canAccessFrom(caller);
    }

    protected boolean checkArgumentsSuitability(@NotNull VMType<?>... args)
    {
        if (this.isSignaturePolymorphic)  // シグネチャ多相メソッドは引数の型を問わない
            return true;

        VMType<?>[] parameterTypes = this.getParameterTypes();
        if (parameterTypes.length != args.length)
            return false;

        boolean allMatch = true;
        for (int i = 0; i < parameterTypes.length; i++)
        {
            if (!parameterTypes[i].equals(args[i]))
            {
                allMatch = false; // 引数の型が一致しない場合
                break;
            }
        }

        return allMatch;
    }

    public boolean isSuitableToCall(@Nullable VMClass caller, @Nullable VMClass owner, @NotNull String methodName,
                                    @Nullable VMType<?> returnType, @NotNull VMType<?>... args)
    {
        return this.checkOwnerSuitability(owner) &&
               this.checkNameSuitability(methodName) &&
               this.checkReturnTypeSuitability(returnType) &&
               this.checkAccessSuitability(caller) &&
               this.checkArgumentsSuitability(args);
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
