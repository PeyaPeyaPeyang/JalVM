package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.compiler.jvm.TypeDescriptor;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMByte;
import tokyo.peya.langjal.vm.values.VMDouble;
import tokyo.peya.langjal.vm.values.VMFloat;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMShort;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.invoke.VMMethodHandleLookupObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.invoke.VMMethodTypeObject;

import java.util.HashMap;
import java.util.Map;

public class DynamicInvocationHelper
{
    private final Map<InvokeDynamicInsnNode, VMObject> resolvedCallSiteCaches;
    private final Map<Handle, VMObject> resolvedHandleCaches;
    private final Map<ConstantDynamic, VMObject> resolvedConstantCaches;


    public DynamicInvocationHelper()
    {
        this.resolvedCallSiteCaches = new HashMap<>();
        this.resolvedHandleCaches = new HashMap<>();
        this.resolvedConstantCaches = new HashMap<>();
    }


    public VMObject resolveCallSite(@NotNull VMFrame frame, @NotNull String name, @NotNull MethodDescriptor descriptor,
                                     @NotNull InvokeDynamicInsnNode operand)
    {
        if (this.resolvedCallSiteCaches.containsKey(operand))
            return this.resolvedCallSiteCaches.get(operand);

        // bsm := (MethodHandle$Lookup lookup, String targetName, MethodType targetType, ...bsmArgs) -> CallSite
        VMClass targetClass = frame.getMethod().getOwningClass();
        VMMethodHandleLookupObject lookup = VMMethodHandleLookupObject.createLookupChain(frame, targetClass);
        VMValue[] bsmParameters = this.createBSMParameters(
                frame.getVm().getClassLoader(),
                lookup,
                name,
                descriptor,
                operand.bsmArgs
        );

        // 引数を解決できたら，Bootstrap Method を呼び出す
        if (this.resolveBSMParameters(frame.getThread(), frame.getVm().getClassLoader(), lookup, bsmParameters))
            this.invokeBootstrapMethod(frame, bsmParameters, operand);

        return null;  // まだ解決されていない場合は null る.
    }

    private boolean resolveBSMParameters(@NotNull VMThread thread,
                                         @NotNull VMSystemClassLoader cl,
                                         @NotNull VMMethodHandleLookupObject lookup, @NotNull VMValue[] bsmParameters)
    {
        for (int i = 0; i < bsmParameters.length; i++)
        {
            VMValue param = bsmParameters[i];
            if (param instanceof UnresolvedConstantDynamic unresolvedConstant)
            {
                VMObject resolved = this.resolveConstantDynamic(lookup, unresolvedConstant);
                if (resolved == null)
                    return false;
                bsmParameters[i] = resolved;
            }
            else if (param instanceof UnresolvedMethodHandle unresolvedHandle)
            {
                VMObject resolved = this.resolveMethodHandle(thread, cl, lookup, unresolvedHandle);
                if (resolved == null)
                    return false;
                bsmParameters[i] = resolved;
            }
        }
        return true;
    }

    @Nullable
    private VMObject resolveConstantDynamic(VMMethodHandleLookupObject lookup,
                                            @NotNull UnresolvedConstantDynamic unresolvedConstant)
    {
        throw new VMPanic("ConstantDynamic resolution is not implemented yet.");
    }

    @Nullable
    private VMObject resolveMethodHandle(@NotNull VMThread thread,
                                         @NotNull VMSystemClassLoader cl,
                                         @NotNull VMMethodHandleLookupObject lookup,
                                         @NotNull UnresolvedMethodHandle unresolved)
    {
        if (this.resolvedHandleCaches.containsKey(unresolved.getHandle()))
            return this.resolvedHandleCaches.get(unresolved.getHandle());

        Handle handle = unresolved.getHandle();
        VMClass caller = lookup.getLookupClass().getRepresentingClass();

        switch (handle.getTag())
        {
            case EOpcodes.H_GETFIELD, EOpcodes.H_GETSTATIC, EOpcodes.H_PUTFIELD, EOpcodes.H_PUTSTATIC ->
                    this.resolveVariableMethodHandle(cl, thread, lookup, caller, unresolved);
            case EOpcodes.H_INVOKEVIRTUAL, EOpcodes.H_INVOKEINTERFACE, EOpcodes.H_INVOKESTATIC ->
                    this.resolveNormalInvocationMethodHandle(cl, thread, lookup, caller, unresolved);
            case EOpcodes.H_INVOKESPECIAL -> this.resolveSpecialInvocationMethodHandle(cl, thread, lookup, caller, unresolved);
            case EOpcodes.H_NEWINVOKESPECIAL -> this.resolveConstructorMethodHandle(cl, thread, lookup, caller, unresolved);
            default -> throw new VMPanic("Unsupported MethodHandle tag: " + handle.getTag());
        }

        return null;
    }

    private void resolveVariableMethodHandle(@NotNull VMSystemClassLoader cl,
                                             @NotNull VMThread thread,
                                             @NotNull VMMethodHandleLookupObject lookup,
                                             @NotNull VMClass caller,
                                             @NotNull UnresolvedMethodHandle unresolved)
    {
        final VMClass lookupClass = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandles$Lookup"));
        final VMClass typeClass = cl.findClass(ClassReference.of("java/lang/Class"));
        final VMClass typeString = cl.findClass(ClassReference.of("java/lang/String"));
        final VMClass typeMethodHandle = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandle"));

        Handle handle = unresolved.getHandle();
        String lookupMethodName = switch (handle.getTag()) {
            case EOpcodes.H_GETFIELD -> "findGetter";
            case EOpcodes.H_GETSTATIC -> "findStaticGetter";
            case EOpcodes.H_PUTFIELD -> "findSetter";
            case EOpcodes.H_PUTSTATIC -> "findStaticSetter";
            default -> throw new VMPanic("Unsupported MethodHandle tag for field: " + handle.getTag());
        };

        VMMethod lookupMethod = lookupClass
                .findSuitableMethod(
                        caller,
                        lookupMethodName,
                        typeMethodHandle,
                        typeClass,
                        typeString,
                        typeClass
                );
        if (lookupMethod == null)
            throw new VMPanic("No suitable MethodHandle factory method found: " + lookupClass.getReference().getFullQualifiedName() + "." + lookupMethodName);

        VMClass ownerClass = cl.findClass(ClassReference.of(handle.getOwner()));
        VMValue name = VMStringObject.createString(cl, handle.getName());
        TypeDescriptor type = TypeDescriptor.parse(handle.getDesc());
        VMClass fieldType = VMType.of(type).linkClass(cl).getLinkedClass();

        thread.invokeInterrupting(
                lookupMethod,
                (callback) -> this.resolvedHandleCaches.put(unresolved.getHandle(), (VMObject) callback),
                lookup,
                ownerClass.getClassObject(),
                name,
                fieldType.getClassObject()
        );
    }

    private void resolveConstructorMethodHandle(@NotNull VMSystemClassLoader cl,
                                                @NotNull VMThread thread,
                                                @NotNull VMMethodHandleLookupObject lookup,
                                                @NotNull VMClass caller,
                                                @NotNull UnresolvedMethodHandle unresolved)
    {
        final VMClass lookupClass = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandles$Lookup"));
        final VMClass typeClass = cl.findClass(ClassReference.of("java/lang/Class"));
        final VMClass typeMethodHandle = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandle"));

        Handle handle = unresolved.getHandle();
        if (handle.getTag() != EOpcodes.H_NEWINVOKESPECIAL)
            throw new VMPanic("Unsupported MethodHandle tag for constructor: " + handle.getTag());
        String lookupMethodName = "findConstructor";

        VMMethod lookupMethod = lookupClass
                .findSuitableMethod(
                        caller,
                        lookupMethodName,
                        typeMethodHandle,
                        typeClass,
                        typeClass
                );
        if (lookupMethod == null)
            throw new VMPanic("No suitable MethodHandle factory method found: " + lookupClass.getReference().getFullQualifiedName() + "." + lookupMethodName);

        VMClass ownerClass = cl.findClass(ClassReference.of(handle.getOwner()));
        TypeDescriptor type = TypeDescriptor.parse(handle.getDesc());
        VMClass fieldType = VMType.of(type).linkClass(cl).getLinkedClass();

        thread.invokeInterrupting(
                lookupMethod,
                (callback) -> this.resolvedHandleCaches.put(unresolved.getHandle(), (VMObject) callback),
                lookup,
                ownerClass.getClassObject(),
                fieldType.getClassObject()
        );
    }

    private void resolveNormalInvocationMethodHandle(@NotNull VMSystemClassLoader cl,
                                                     @NotNull VMThread thread,
                                                     @NotNull VMMethodHandleLookupObject lookup,
                                                     @NotNull VMClass caller,
                                                     @NotNull UnresolvedMethodHandle unresolved)
    {
        final VMClass lookupClass = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandles$Lookup"));
        final VMClass typeClass = cl.findClass(ClassReference.of("java/lang/Class"));
        final VMClass typeString = cl.findClass(ClassReference.of("java/lang/String"));
        final VMClass typeMethodType = cl.findClass(ClassReference.of("java/lang/invoke/MethodType"));
        final VMClass typeMethodHandle = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandle"));

        Handle handle = unresolved.getHandle();
        String lookupMethodName = switch (handle.getTag()) {
            case EOpcodes.H_INVOKEVIRTUAL, EOpcodes.H_INVOKEINTERFACE -> "findVirtual";
            case EOpcodes.H_INVOKESTATIC -> "findStatic";
            default -> throw new VMPanic("Unsupported MethodHandle tag for field: " + handle.getTag());
        };

        VMMethod lookupMethod = lookupClass
                .findSuitableMethod(
                        caller,
                        lookupMethodName,
                        typeMethodHandle,
                        typeClass,
                        typeString,
                        typeMethodType
                );
        if (lookupMethod == null)
            throw new VMPanic("No suitable MethodHandle factory method found: " + lookupClass.getReference().getFullQualifiedName() + "." + lookupMethodName);

        VMClass ownerClass = cl.findClass(ClassReference.of(handle.getOwner()));
        VMValue name = VMStringObject.createString(cl, handle.getName());
        MethodDescriptor desc = MethodDescriptor.parse(handle.getDesc());
        VMMethodTypeObject methodType = VMMethodTypeObject.of(cl, desc);

        thread.invokeInterrupting(
                lookupMethod,
                (callback) -> this.resolvedHandleCaches.put(unresolved.getHandle(), (VMObject) callback),
                lookup,
                ownerClass.getClassObject(),
                name,
                methodType
        );
    }

    private void resolveSpecialInvocationMethodHandle(@NotNull VMSystemClassLoader cl,
                                                      @NotNull VMThread thread,
                                                      @NotNull VMMethodHandleLookupObject lookup,
                                                      @NotNull VMClass caller,
                                                      @NotNull UnresolvedMethodHandle unresolved)
    {
        final VMClass lookupClass = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandles$Lookup"));
        final VMClass typeClass = cl.findClass(ClassReference.of("java/lang/Class"));
        final VMClass typeString = cl.findClass(ClassReference.of("java/lang/String"));
        final VMClass typeMethodType = cl.findClass(ClassReference.of("java/lang/invoke/MethodType"));
        final VMClass typeMethodHandle = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandle"));

        Handle handle = unresolved.getHandle();
        if (handle.getTag() != EOpcodes.H_INVOKESPECIAL)
            throw new VMPanic("Unsupported MethodHandle tag for constructor: " + handle.getTag());
        String lookupMethodName = "findSpecial";

        VMMethod lookupMethod = lookupClass
                .findSuitableMethod(
                        caller,
                        lookupMethodName,
                        typeMethodHandle,
                        typeClass,
                        typeString,
                        typeMethodType,
                        typeClass
                );
        if (lookupMethod == null)
            throw new VMPanic("No suitable MethodHandle factory method for special invocation found: " + lookupClass.getReference().getFullQualifiedName() + "." + lookupMethodName);

        VMClass ownerClass = cl.findClass(ClassReference.of(handle.getOwner()));
        VMValue name = VMStringObject.createString(cl, handle.getName());
        MethodDescriptor desc = MethodDescriptor.parse(handle.getDesc());
        VMMethodTypeObject methodType = VMMethodTypeObject.of(cl, desc);

        thread.invokeInterrupting(
                lookupMethod,
                (callback) -> this.resolvedHandleCaches.put(unresolved.getHandle(), (VMObject) callback),
                lookup,
                ownerClass.getClassObject(),
                name,
                methodType,
                caller.getClassObject()
        );
    }

    private void invokeBootstrapMethod(@NotNull VMFrame frame, @NotNull VMValue[] bsmParameters, @NotNull InvokeDynamicInsnNode operand)
    {
        Handle bsmHandle = operand.bsm;
        VMClass bsmClass = frame.getVm().getClassLoader().findClass(ClassReference.of(bsmHandle.getOwner()));
        String name = bsmHandle.getName();
        MethodDescriptor bsmDescriptor = MethodDescriptor.parse(bsmHandle.getDesc());

        if (!bsmClass.isInitialised())
        {
            bsmClass.initialise(frame.getThread());// クラスが初期化されていない場合は初期化。
            return;
        }

        String ownerName = bsmClass.getReference().getFullQualifiedName();
        VMClass caller = frame.getMethod().getClazz();
        InvocationHelper.InvocationContext ctxt = InvocationHelper.retrieveCtxt(ownerName, bsmDescriptor, frame);
        VMMethod method = bsmClass.findSuitableMethod(
                caller,
                bsmClass,
                name,
                null,
                ctxt.argumentTypes()
        );
        if (method == null)
            throw new VMPanic("No suitable bootstrap method found: " + ownerName + "." + name + bsmDescriptor);

        if (!method.getAccessAttributes().has(AccessAttribute.STATIC))
            throw new VMPanic("Bootstrap method is not static: " + ownerName + "." + name + bsmDescriptor);

        // bootstrap method を割り込み実行して，CallSite を得る
        frame.getThread().invokeInterrupting(method, (callback) -> {
            if (!(callback instanceof VMObject mayCallSite))
                throw new VMPanic("Bootstrap method did not return CallSite: " + ownerName + "." + name + bsmDescriptor);
            if (!mayCallSite.getObjectType().getReference().isEqualClassName("java/lang/invoke/CallSite"))
                throw new VMPanic("Bootstrap method did not return CallSite: " + ownerName + "." + name + bsmDescriptor);

            this.resolvedCallSiteCaches.put(operand, mayCallSite);
        }, bsmParameters);
    }

    private VMValue[] createBSMParameters(@NotNull VMSystemClassLoader cl, @NotNull VMMethodHandleLookupObject lookup,
                                          @NotNull String name, @NotNull MethodDescriptor descriptor,
                                          @NotNull Object[] bsmArgs)
    {
        final int RESERVED = 3;

        VMValue[] bsmParameters = new VMValue[RESERVED + bsmArgs.length];
        bsmParameters[0] = lookup;
        bsmParameters[1] = VMStringObject.createString(cl, name);
        bsmParameters[2] = VMMethodTypeObject.of(cl, descriptor);

        for (int i = 0; i < bsmArgs.length; i++)
        {
            Object arg = bsmArgs[i];
            int slot = RESERVED + i;
            bsmParameters[slot] = this.createOneBSMParameter(cl, arg);
        }

        return bsmParameters;
    }

    private VMValue createOneBSMParameter(@NotNull VMSystemClassLoader cl, @NotNull Object arg)
    {
        return switch (arg)
        {
            case Type type -> createTypeBSMParameter(cl, type);
            case Handle handle -> {
                if (this.resolvedHandleCaches.containsKey(handle))
                    yield this.resolvedHandleCaches.get(handle);
                yield new UnresolvedMethodHandle(handle);
            }
            case String str -> VMStringObject.createString(cl, str);
            case Integer i -> new VMInteger(i);
            case Short s -> new VMShort(s);
            case Byte b -> new VMByte(b);
            case Double d -> new VMDouble(d);
            case Float f -> new VMFloat(f);
            case Long l -> new VMLong(l);
            case ConstantDynamic c -> {
                if (this.resolvedConstantCaches.containsKey(c))
                    yield this.resolvedConstantCaches.get(c);
                yield new UnresolvedConstantDynamic(c);
            }
            default -> throw new IllegalArgumentException("Unsupported bsmArg type: " + arg.getClass().getName());
        };
    }

    private static VMValue createTypeBSMParameter(@NotNull VMSystemClassLoader cl, @NotNull Type type)
    {
        if (type.getSort() == Type.METHOD)
            return VMMethodTypeObject.of(cl, MethodDescriptor.parse(type.getDescriptor()));
        return VMType.convertASMType(type).getLinkedClass().getClassObject();
    }

    @Getter
    private static class UnresolvedMethodHandle implements VMValue
    {
        private final Handle handle;

        public UnresolvedMethodHandle(@NotNull Handle handle)
        {
            this.handle = handle;
        }

        @Override
        public @NotNull VMType<?> type()
        {
            return VMType.ofClassName("java/lang/invoke/MethodHandle");
        }

        @Override
        public boolean isCompatibleTo(@NotNull VMValue other)
        {
            return false;
        }

        @Override
        public @NotNull VMValue cloneValue()
        {
            return this;
        }
    }

    @Getter
    private static class UnresolvedConstantDynamic implements VMValue
    {
        private final ConstantDynamic constantDynamic;

        public UnresolvedConstantDynamic(@NotNull ConstantDynamic constantDynamic)
        {
            this.constantDynamic = constantDynamic;
        }

        @Override
        public @NotNull VMType<?> type()
        {
            return VMType.ofClassName("java/lang/invoke/MethodHandle");
        }

        @Override
        public boolean isCompatibleTo(@NotNull VMValue other)
        {
            return false;
        }

        @Override
        public @NotNull VMValue cloneValue()
        {
            return this;
        }
    }
}
