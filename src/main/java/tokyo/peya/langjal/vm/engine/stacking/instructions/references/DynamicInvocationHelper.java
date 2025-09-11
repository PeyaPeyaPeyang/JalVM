package tokyo.peya.langjal.vm.engine.stacking.instructions.references;

import lombok.AllArgsConstructor;
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
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMByte;
import tokyo.peya.langjal.vm.values.VMDouble;
import tokyo.peya.langjal.vm.values.VMFloat;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMShort;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.invoke.VMMethodHandleLookupObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DynamicInvocationHelper
{
    private final Map<InvokeDynamicInsnNode, VMObject> resolvedCallSiteCaches;
    private final Map<Handle, VMObject> resolvedHandleCaches;
    private final Map<ConstantDynamic, VMObject> resolvedConstantCaches;
    private final Map<MethodDescriptor, VMObject> resolvedMethodTypeCaches;

    public DynamicInvocationHelper()
    {
        this.resolvedCallSiteCaches = new HashMap<>();
        this.resolvedHandleCaches = new HashMap<>();
        this.resolvedConstantCaches = new HashMap<>();
        this.resolvedMethodTypeCaches = new HashMap<>();
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
                frame.getVM(),
                lookup,
                name,
                descriptor,
                operand.bsmArgs
        );

        // 引数を解決できたら，Bootstrap Method を呼び出す
        if (this.resolveBSMParameters(frame.getThread(), frame.getClassLoader(), lookup, bsmParameters))
            this.invokeBootstrapMethod(frame, bsmParameters, operand);

        return null;  // まだ解決されていない場合は null る.
    }

    private boolean resolveBSMParameters(@NotNull VMThread thread,
                                         @NotNull VMSystemClassLoader cl,
                                         @NotNull VMMethodHandleLookupObject lookup, @NotNull VMValue[] bsmParameters)
    {
        nextParam: for (int i = 0; i < bsmParameters.length; i++)
        {
            VMValue param = bsmParameters[i];
            VMObject resolved;

            // default で for を continue するので，switch 式は使えない
            switch (param) {
                case UnresolvedConstantDynamic unresolvedConstant:
                    resolved = this.resolveConstantDynamic(lookup, unresolvedConstant);
                    break;
                case UnresolvedMethodType unresolvedType:
                    resolved = this.resolveMethodType(thread, unresolvedType);
                    break;
                case UnresolvedMethodHandle unresolvedHandle:
                    resolved = this.resolveMethodHandle(thread, lookup, unresolvedHandle);
                    break;
                default:
                    continue nextParam;  // 既に解決されている場合
            };

            if (resolved == null)
                return false;
            bsmParameters[i] = resolved;
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
    private VMObject resolveMethodType(@NotNull VMThread thread, UnresolvedMethodType unresolved)
    {
        return this.resolveMethodType(thread, unresolved.getDescriptor());
    }

    @Nullable
    private VMObject resolveMethodType(@NotNull VMThread thread,  @NotNull MethodDescriptor descriptor)
    {
        if (this.resolvedMethodTypeCaches.containsKey(descriptor))
            return this.resolvedMethodTypeCaches.get(descriptor);

        // MethodType.methodType(Class returnType, Class[] parameterTypes, boolean trusted) を取得
        VMSystemClassLoader cl = thread.getVM().getClassLoader();
        final VMClass methodTypeClass = cl.findClass(ClassReference.of("java/lang/invoke/MethodType"));
        final MethodDescriptor methodTypeDescriptor = MethodDescriptor.parse(
                "(Ljava/lang/Class;[Ljava/lang/Class;Z)Ljava/lang/invoke/MethodType;"
        );
        final VMMethod methodTypeFactory = methodTypeClass.findMethod("methodType", methodTypeDescriptor);
        if (methodTypeFactory == null)
            throw new VMPanic("No suitable MethodType factory method found: " + methodTypeClass.getReference().getFullQualifiedName() + ".methodType");

        // 戻り値の型を取得
        VMClassObject returnType = VMType.of(thread, descriptor.getReturnType())
                                         .getLinkedClass()
                                         .getClassObject();
        // 引数の型配列を作成
        TypeDescriptor[] paramTypes = descriptor.getParameterTypes();
        VMClassObject[] parameterClasses = new VMClassObject[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++)
            parameterClasses[i] = VMType.of(thread, paramTypes[i]).getLinkedClass().getClassObject();
        VMArray paramArray = new VMArray(
                cl,
                cl.findClass(ClassReference.of("java/lang/Class")),
                parameterClasses
        );

        thread.invokeInterrupting(
                methodTypeFactory,
                (v) -> this.resolvedMethodTypeCaches.put(descriptor, (VMObject) v),
                null,
                returnType,
                paramArray,
                VMBoolean.ofFalse(cl)
        );
        return null;

    }

    @Nullable
    private VMObject resolveMethodHandle(@NotNull VMThread thread,
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
                    this.resolveVariableMethodHandle(thread, lookup, caller, unresolved);
            case EOpcodes.H_INVOKEVIRTUAL, EOpcodes.H_INVOKEINTERFACE, EOpcodes.H_INVOKESTATIC ->
                    this.resolveNormalInvocationMethodHandle(thread, lookup, caller, unresolved);
            case EOpcodes.H_INVOKESPECIAL -> this.resolveSpecialInvocationMethodHandle(thread, lookup, caller, unresolved);
            case EOpcodes.H_NEWINVOKESPECIAL -> this.resolveConstructorMethodHandle(thread, lookup, caller, unresolved);
            default -> throw new VMPanic("Unsupported MethodHandle tag: " + handle.getTag());
        }

        return null;
    }

    private void resolveVariableMethodHandle(@NotNull VMThread thread,
                                             @NotNull VMMethodHandleLookupObject lookup,
                                             @NotNull VMClass caller,
                                             @NotNull UnresolvedMethodHandle unresolved)
    {
        VMSystemClassLoader cl = thread.getClassLoader();
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
        VMValue name = VMStringObject.createString(thread, handle.getName());
        TypeDescriptor type = TypeDescriptor.parse(handle.getDesc());
        VMClass fieldType = VMType.of(thread, type).getLinkedClass();

        thread.invokeInterrupting(
                lookupMethod,
                (callback) -> this.resolvedHandleCaches.put(unresolved.getHandle(), (VMObject) callback),
                lookup,
                ownerClass.getClassObject(),
                name,
                fieldType.getClassObject()
        );
    }

    private void resolveConstructorMethodHandle(@NotNull VMThread thread,
                                                @NotNull VMMethodHandleLookupObject lookup,
                                                @NotNull VMClass caller,
                                                @NotNull UnresolvedMethodHandle unresolved)
    {
        JalVM vm = thread.getVM();
        VMSystemClassLoader cl = vm.getClassLoader();

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
        VMClass fieldType = VMType.of(vm ,type).getLinkedClass();

        thread.invokeInterrupting(
                lookupMethod,
                (callback) -> this.resolvedHandleCaches.put(unresolved.getHandle(), (VMObject) callback),
                lookup,
                ownerClass.getClassObject(),
                fieldType.getClassObject()
        );
    }

    private void resolveNormalInvocationMethodHandle(@NotNull VMThread thread,
                                                     @NotNull VMMethodHandleLookupObject lookup,
                                                     @NotNull VMClass caller,
                                                     @NotNull UnresolvedMethodHandle unresolved)
    {
        Handle handle = unresolved.getHandle();
        MethodDescriptor desc = MethodDescriptor.parse(handle.getDesc());
        VMObject methodType = this.resolveMethodType(thread, desc);
        if (methodType == null)
            return;  // 次のループで解決されるまで待つ

        VMSystemClassLoader cl = thread.getClassLoader();
        final VMClass lookupClass = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandles$Lookup"));
        final VMClass typeClass = cl.findClass(ClassReference.of("java/lang/Class"));
        final VMClass typeString = cl.findClass(ClassReference.of("java/lang/String"));
        final VMClass typeMethodType = cl.findClass(ClassReference.of("java/lang/invoke/MethodType"));
        final VMClass typeMethodHandle = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandle"));

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
        VMValue name = VMStringObject.createString(thread, handle.getName());

        thread.invokeInterrupting(
                lookupMethod,
                (callback) -> this.resolvedHandleCaches.put(unresolved.getHandle(), (VMObject) callback),
                lookup,
                ownerClass.getClassObject(),
                name,
                methodType
        );
    }

    private void resolveSpecialInvocationMethodHandle(@NotNull VMThread thread,
                                                      @NotNull VMMethodHandleLookupObject lookup,
                                                      @NotNull VMClass caller,
                                                      @NotNull UnresolvedMethodHandle unresolved)
    {
        Handle handle = unresolved.getHandle();
        MethodDescriptor desc = MethodDescriptor.parse(handle.getDesc());
        VMObject methodType = this.resolveMethodType(thread, desc);
        if (methodType == null)
            return;  // 次のループで解決されるまで待つ

        VMSystemClassLoader cl = thread.getClassLoader();
        final VMClass lookupClass = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandles$Lookup"));
        final VMClass typeClass = cl.findClass(ClassReference.of("java/lang/Class"));
        final VMClass typeString = cl.findClass(ClassReference.of("java/lang/String"));
        final VMClass typeMethodType = cl.findClass(ClassReference.of("java/lang/invoke/MethodType"));
        final VMClass typeMethodHandle = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandle"));

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
        VMValue name = VMStringObject.createString(thread, handle.getName());
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
        VMClass bsmClass = frame.getClassLoader().findClass(ClassReference.of(bsmHandle.getOwner()));
        String name = bsmHandle.getName();
        MethodDescriptor bsmDescriptor = MethodDescriptor.parse(bsmHandle.getDesc());

        if (!bsmClass.isInitialised())
        {
            bsmClass.initialise(frame.getThread());// クラスが初期化されていない場合は初期化。
            return;
        }

        String ownerName = bsmClass.getReference().getFullQualifiedName();
        VMClass caller = frame.getMethod().getClazz();
        VMMethod method = bsmClass.findSuitableMethod(
                caller,
                bsmClass,
                name,
                null,
                Arrays.stream(bsmParameters).map(VMValue::type).toArray(VMType[]::new)
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

    private VMValue[] createBSMParameters(@NotNull JalVM vm, @NotNull VMMethodHandleLookupObject lookup,
                                          @NotNull String name, @NotNull MethodDescriptor descriptor,
                                          @NotNull Object[] bsmArgs)
    {
        final int RESERVED = 3;

        VMValue[] bsmParameters = new VMValue[RESERVED + bsmArgs.length];
        bsmParameters[0] = lookup;
        bsmParameters[1] = VMStringObject.createString(vm, name);
        bsmParameters[2] = this.retrieveMethodType(vm, descriptor);

        for (int i = 0; i < bsmArgs.length; i++)
        {
            Object arg = bsmArgs[i];
            int slot = RESERVED + i;
            bsmParameters[slot] = this.createOneBSMParameter(vm, arg);
        }

        return bsmParameters;
    }

    private VMValue createOneBSMParameter(@NotNull JalVM vm, @NotNull Object arg)
    {
        return switch (arg)
        {
            case Type type -> this.createTypeBSMParameter(vm, type);
            case Handle handle -> {
                if (this.resolvedHandleCaches.containsKey(handle))
                    yield this.resolvedHandleCaches.get(handle);
                yield new UnresolvedMethodHandle(vm, handle);
            }
            case String str -> VMStringObject.createString(vm, str);
            case Integer i -> new VMInteger(vm, i);
            case Short s -> new VMShort(vm, s);
            case Byte b -> new VMByte(vm, b);
            case Double d -> new VMDouble(vm, d);
            case Float f -> new VMFloat(vm, f);
            case Long l -> new VMLong(vm, l);
            case ConstantDynamic c -> {
                if (this.resolvedConstantCaches.containsKey(c))
                    yield this.resolvedConstantCaches.get(c);
                yield new UnresolvedConstantDynamic(vm, c);
            }
            default -> throw new IllegalArgumentException("Unsupported bsmArg type: " + arg.getClass().getName());
        };
    }

    private VMValue createTypeBSMParameter(@NotNull JalVM vm, @NotNull Type type)
    {
        if (type.getSort() == Type.METHOD)
            return this.retrieveMethodType(vm, MethodDescriptor.parse(type.getDescriptor()));
        return VMType.convertASMType(vm, type).getLinkedClass().getClassObject();
    }

    private VMValue retrieveMethodType(@NotNull JalVM vm, @NotNull MethodDescriptor descriptor)
    {
        if (this.resolvedMethodTypeCaches.containsKey(descriptor))
            return this.resolvedMethodTypeCaches.get(descriptor);

        return new UnresolvedMethodType(vm, descriptor);
    }

    @Getter
    @AllArgsConstructor
    private static class UnresolvedMethodHandle implements VMValue
    {
        private final JalVM vm;
        private final Handle handle;

        @Override
        public @NotNull VMType<?> type()
        {
            return VMType.ofClassName(this.vm, "java/lang/invoke/MethodHandle");
        }

        @Override
        public int identityHashCode()
        {
            return -1;
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
    @AllArgsConstructor
    private static class UnresolvedConstantDynamic implements VMValue
    {
        private final JalVM vm;
        private final ConstantDynamic constantDynamic;

        @Override
        public int identityHashCode()
        {
            return -1;
        }

        @Override
        public @NotNull VMType<?> type()
        {
            return VMType.ofClassName(this.vm, "java/lang/invoke/ConstantDynamic");
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
    @AllArgsConstructor
    private static class UnresolvedMethodType implements VMValue
    {
        private final JalVM vm;
        private final MethodDescriptor descriptor;

        @Override
        public @NotNull VMType<?> type()
        {
            return VMType.ofClassName(this.vm, "java/lang/invoke/MethodType");
        }

        @Override
        public int identityHashCode()
        {
            return -1;
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
