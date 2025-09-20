package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.invoke.VMResolvedMethodName;

import java.util.HashMap;
import java.util.Map;

public class InjectorMethodHandle implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/invoke/MethodHandle");

    private final Map<Integer, VMValue> resultCache;

    private /* final */ VMClass fieldAccessor;
    private /* final */ VMClass fieldAccessorStatic;
    private /* final */ VMClass constructorAccessor;
    private /* final */ VMClass boundMethodHandle;

    public InjectorMethodHandle(@NotNull VMSystemClassLoader cl)
    {
        this.resultCache = new HashMap<>();

    }

    @Override
    public @NotNull ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        this.fieldAccessor = cl.findClass(ClassReference.of("java/lang/invoke/DirectMethodHandle$Accessor"));
        this.fieldAccessorStatic = cl.findClass(ClassReference.of("java/lang/invoke/DirectMethodHandle$StaticAccessor"));
        this.constructorAccessor = cl.findClass(ClassReference.of("java/lang/invoke/DirectMethodHandle$Constructor"));
        this.boundMethodHandle = cl.findClass(ClassReference.of("java/lang/invoke/BoundMethodHandle"));

        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_FINAL | EOpcodes.ACC_VARARGS | EOpcodes.ACC_NATIVE,
                        "invokeExact",
                        "([Ljava/lang/Object;)Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    // @SignaturePolymorphic らしいので，戻り値と引数の型は問わない
                    @Override
                    protected boolean checkReturnTypeSuitability(@Nullable VMType<?> returnType)
                    {
                        return true;
                    }

                    @Override
                    protected boolean checkArgumentsSuitability(@NotNull VMType<?>... args)
                    {
                        return true;
                    }

                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return InjectorMethodHandle.this.invoke(frame, caller, instance, args);
                    }
                }
        );

        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_FINAL | EOpcodes.ACC_VARARGS | EOpcodes.ACC_NATIVE,
                        "invokeBasic",
                        "([Ljava/lang/Object;)Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    // @SignaturePolymorphic らしいので，戻り値と引数の型は問わない
                    @Override
                    protected boolean checkReturnTypeSuitability(@Nullable VMType<?> returnType)
                    {
                        return true;
                    }

                    @Override
                    protected boolean checkArgumentsSuitability(@NotNull VMType<?>... args)
                    {
                        return true;
                    }

                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return InjectorMethodHandle.this.invoke(frame, caller, instance, args);
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_STATIC | EOpcodes.ACC_VARARGS | EOpcodes.ACC_NATIVE,
                        "linkToStatic",
                        "([Ljava/lang/Object;)Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    // @SignaturePolymorphic らしいので，戻り値と引数の型は問わない
                    @Override
                    protected boolean checkReturnTypeSuitability(@Nullable VMType<?> returnType)
                    {
                        return true;
                    }

                    @Override
                    protected boolean checkArgumentsSuitability(@NotNull VMType<?>... args)
                    {
                        return true;
                    }

                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return InjectorMethodHandle.this.accessLinkToMethod(frame, args, null);
                    }
                }
        );
    }

    private VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                           @Nullable VMObject instance, @NotNull VMValue[] args)
    {
        if (instance == null)
            throw new VMPanic("Cannot invoke MethodHandle method on null instance");

        if (this.fieldAccessor.isInstance(instance))
            return accessInstanceField(instance, (VMObject) args[0]);
        if (this.fieldAccessorStatic.isInstance(instance))
            return accessStaticField(frame, instance);
        if (this.constructorAccessor.isInstance(instance))
            return accessConstructor(frame, caller, instance, args);
        if (this.boundMethodHandle.isInstance(instance))
            return accessBoundMethodHandle(frame, instance, args);

        return accessNormalInvocation(frame, instance, args);
    }

    private VMValue invokeFieldGetter(@NotNull VMComponent com, @NotNull VMObject getter, @Nullable VMObject instance)
    {
        if (this.fieldAccessorStatic.isInstance(getter))
            return accessStaticField(com, getter);
        if (this.fieldAccessor.isInstance(getter))
            return accessInstanceField(getter, instance);

        throw new VMPanic("Not a field accessor: " + getter.getObjectType().getReference().toString());
    }

    private void invokeMethodWithOptionalThis(@NotNull VMFrame frame,
                                                          @NotNull VMMethod method,
                                                          @NotNull VMValue[] args)
    {
        VMValue[] invokeArgs = this.obtainArguments(method, args);

        VMObject thisObject = method.getAccessAttributes().has(AccessAttribute.STATIC) ? null: obtainThis(args, method);
        if (!method.getAccessAttributes().has(AccessAttribute.STATIC))
            invokeArgs = removeFirstArg(invokeArgs);

        frame.getThread().invokeMethod(
                method,
                false,
                thisObject,
                invokeArgs
        );

    }

    private @Nullable VMValue accessBoundMethodHandle(@NotNull VMFrame frame, @NotNull VMObject methodHandle,
                                                      @NotNull VMValue[] args)
    {
        VMValue cached = this.checkCache(methodHandle);
        if (cached != null)
            return cached;

        VMObject lambdaForm = (VMObject) methodHandle.getField("form");
        VMObject vmEntry = (VMObject) lambdaForm.getField("vmentry");
        VMResolvedMethodName resolved = (VMResolvedMethodName) vmEntry.getField("method");
        VMMethod method = resolved.getMethod();

        VMValue[] newArgs = new VMValue[1 + args.length];
        newArgs[0] = methodHandle;
        System.arraycopy(args, 0, newArgs, 1, args.length);

        VMValue[] invokeArgs = this.obtainArguments(method, newArgs);

        VMObject thisObject = method.getAccessAttributes().has(AccessAttribute.STATIC) ? null: obtainThis(args, method);
        if (thisObject != null)
            invokeArgs = removeFirstArg(invokeArgs);

        frame.getThread().invokeMethod(
                method,
                false,
                thisObject,
                invokeArgs
        );

        return null;
    }

    private @Nullable VMValue accessNormalInvocation(@NotNull VMFrame frame, @Nullable VMObject methodHandle,
                                                     @NotNull VMValue[] args)
    {
        if (methodHandle == null)
            throw new VMPanic("MethodHandle instance is null");

        VMValue cached = this.checkCache(methodHandle);
        if (cached != null)
            return cached;

        VMObject member = (VMObject) methodHandle.getField("member");
        VMResolvedMethodName resolved = (VMResolvedMethodName) member.getField("method");
        VMMethod method = resolved.getMethod();

        this.invokeMethodWithOptionalThis(frame, method, args);
        return null;
    }

    private @NotNull VMValue accessConstructor(@NotNull VMFrame frame, @NotNull VMClass caller,
                                               @NotNull VMObject instance, @NotNull VMValue[] args)
    {
        VMObject initMethod = (VMObject) instance.getField("initMethod");
        VMClassObject instanceClass = (VMClassObject) instance.getField("instanceClass");
        VMResolvedMethodName resolved = (VMResolvedMethodName) initMethod.getField("method");

        VMMethod method = resolved.getMethod();
        VMValue[] invokeArgs = this.obtainArguments(method, args);

        VMClass thisClass = instanceClass.getRepresentingClass();
        VMObject newInstance = thisClass.createInstance();

        newInstance.initialiseInstance(frame, caller, method, invokeArgs, true);
        return newInstance;
    }

    @Nullable
    private VMValue accessLinkToMethod(@NotNull VMFrame frame, @NotNull VMValue[] args,
                                                 @Nullable VMObject thisObject)
    {
        if (args.length < 1)
            throw new VMPanic("Not enough arguments for linkToVirtual");

        VMObject methodMemberName = (VMObject) args[args.length - 1];
        VMValue cached = this.checkCache(methodMemberName);
        if (cached != null)
            return cached;

        VMResolvedMethodName resolved = (VMResolvedMethodName) methodMemberName.getField("method");
        VMMethod method = resolved.getMethod();

        // 最後の引数はメソッド名なので除外
        VMValue[] invokeArgs = new VMValue[args.length - 1];
        System.arraycopy(args, 0, invokeArgs, 0, invokeArgs.length);

        if (thisObject == null && !method.getAccessAttributes().has(AccessAttribute.STATIC))
            throw new VMPanic("No instance for non-static method");

        this.invokeMethodWithOptionalThis(frame, method, invokeArgs);
        return null;
    }

    private VMValue[] obtainArguments(@NotNull VMMethod method, @NotNull VMValue[] args)
    {
        VMType<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != args.length)
            throw new VMPanic("Argument count mismatch: expected " + paramTypes.length + ", got " + args.length);

        VMValue[] invokeArgs = new VMValue[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++)
            invokeArgs[i] = args[i].conformValue(paramTypes[i]);

        return invokeArgs;
    }

    private void setResultCache(@NotNull VMObject instance, @NotNull VMValue result)
    {
        this.resultCache.put(instance.identityHashCode(), result);
    }

    private VMValue checkCache(@NotNull VMObject instance)
    {
        return this.resultCache.remove(instance.identityHashCode());
    }

    private static @NotNull VMValue accessInstanceField(@NotNull VMObject getter, @Nullable VMObject instance)
    {
        if (instance == null)
            throw new VMPanic("Instance field accessor requires non-null instance");

        int instanceOffset = ((VMInteger) getter.getField("fieldOffset")).asNumber().intValue();
        VMField field = instance.getObjectType().findField(instanceOffset);

        VMClass fieldType = ((VMClassObject) getter.getField("fieldType")).getRepresentingClass();
        return instance.getField(field).conformValue(fieldType);
    }

    private static @NotNull VMValue accessStaticField(@NotNull VMComponent com, @NotNull VMObject instance)
    {
        VMClass fieldType = ((VMClassObject) instance.getField("fieldType")).getRepresentingClass();
        long staticOffset = ((VMLong) instance.getField("staticOffset")).asNumber().longValue();
        VMField field = com.getVM().getHeap().getStaticFieldByID(staticOffset);
        if (field == null)
            throw new NullPointerException("Static field not found by ID: " + staticOffset + " (field type: " + fieldType.getReference()
                                                                                                                         .toString() + ")");

        return field.getClazz().getStaticFieldValue(field);
    }

    private static VMObject obtainThis(@Nullable VMValue[] args, VMMethod method)
    {
        if (method.getAccessAttributes()
                  .has(AccessAttribute.STATIC)) return null;
        if (args == null || args.length == 0) throw new VMPanic("No argument for non-static method");
        VMClass methodOwner = method.getClazz();
        VMValue firstArg = args[0];
        if (!methodOwner.isInstance(firstArg))
            throw new VMPanic("Method owner type mismatch: expected " + methodOwner.getReference() + ", got " + firstArg);
        return (VMObject) firstArg;
    }

    private static VMValue[] removeFirstArg(@Nullable VMValue[] args)
    {
        if (args == null || args.length == 0) return new VMValue[0];
        if (args.length == 1) return new VMValue[0];
        VMValue[] newArgs = new VMValue[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        return newArgs;
    }
}
