package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
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

    public InjectorMethodHandle()
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
        VMClass fieldAccessor = cl.findClass(ClassReference.of("java/lang/invoke/DirectMethodHandle$Accessor"));
        VMClass fieldAccessorStatic = cl.findClass(ClassReference.of("java/lang/invoke/DirectMethodHandle$StaticAccessor"));
        VMClass constructorAccessor = cl.findClass(ClassReference.of("java/lang/invoke/DirectMethodHandle$Constructor"));

        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_FINAL | EOpcodes.ACC_NATIVE,
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
                        assert instance != null;
                        if (fieldAccessor.isInstance(instance))
                            return accessInstanceField();
                        else if (fieldAccessorStatic.isInstance(instance))
                            return accessStaticField(frame, instance);
                        else if (constructorAccessor.isInstance(instance))
                            return InjectorMethodHandle.this.accessConstructor(frame, caller, instance, args);

                        throw new UnsupportedOperationException("Unsupported MethodHandle type: " + instance.getClass().getName());
                    }
                }
        );

        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_FINAL | EOpcodes.ACC_NATIVE,
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
                        assert instance != null;

                        if (fieldAccessor.isInstance(instance))
                            return accessInstanceField();
                        else if (fieldAccessorStatic.isInstance(instance))
                            return accessStaticField(frame, instance);
                        else if (constructorAccessor.isInstance(instance))
                            return InjectorMethodHandle.this.accessConstructor(frame, caller, instance, args);

                        throw new UnsupportedOperationException("Unsupported MethodHandle type: " + instance.getClass().getName());
                    }
                }
        );
    }

    private static @NotNull VMValue accessInstanceField()
    {
        throw new UnsupportedOperationException();
    }

    private static @NotNull VMValue accessStaticField(@NotNull VMFrame frame, @NotNull VMObject instance)
    {
        VMClass fieldType = ((VMClassObject) instance.getField("fieldType")).getRepresentingClass();
        long staticOffset = ((VMLong) instance.getField("staticOffset")).asNumber().longValue();
        VMField field = frame.getVM().getHeap().getStaticFieldByID(staticOffset);
        if (field == null)
            throw new NullPointerException("Static field not found by ID: " + staticOffset + " (field type: " + fieldType.getReference().toString() + ")");

        return field.getClazz().getStaticFieldValue(field);
    }

    private @Nullable VMValue accessInvocation(@NotNull VMFrame frame, @NotNull VMObject instance, @NotNull VMValue[] args)
    {
        VMValue cached = this.checkCache(instance);
        if (cached != null)
            return cached;

        VMObject initMethod = (VMObject) instance.getField("initMethod");
        VMClassObject instanceClass = (VMClassObject) instance.getField("instanceClass");
        VMResolvedMethodName resolved = (VMResolvedMethodName) initMethod.getField("method");

        VMMethod method = resolved.getMethod();
        VMValue[] invokeArgs = obtainArguments(args, method);

        VMObject thisObject;  // this をセットする
        if (method.getAccessAttributes().has(AccessAttribute.STATIC))
            thisObject = null;
        else
        {
            if (invokeArgs.length == 0)
                throw new VMPanic("No argument for constructor");
            thisObject = obtainThis(args, method);
            invokeArgs = removeFirstArg(invokeArgs);
        }

        frame.rerunInstruction();
        frame.getThread().invokeInterrupting(
                method,
                v -> this.setResultCache(instance, v),
                thisObject,
                invokeArgs
        );

        return null;
    }

    private static VMValue[] obtainArguments(@NotNull VMValue[] args, VMMethod method)
    {
        VMType<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != args.length)
            throw new VMPanic("Argument count mismatch: expected " + paramTypes.length + ", got " + args.length);

        VMValue[] invokeArgs = new VMValue[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++)
            invokeArgs[i] = args[0].conformValue(paramTypes[i]);
        return invokeArgs;
    }

    private static VMObject obtainThis(@Nullable VMValue[] args, VMMethod method)
    {
        if (method.getAccessAttributes().has(AccessAttribute.STATIC))
            return null;
        if (args == null || args.length == 0)
            throw new VMPanic("No argument for non-static method");
        VMClass methodOwner = method.getClazz();
        VMValue firstArg = args[0];
        if (!methodOwner.isInstance(firstArg))
            throw new VMPanic("Method owner type mismatch: expected " + methodOwner.getReference()
                                      + ", got " + firstArg);
        return (VMObject) firstArg;
    }

    private static VMValue[] removeFirstArg(@Nullable VMValue[] args)
    {
        if (args == null || args.length == 0)
            return new VMValue[0];
        if (args.length == 1)
            return new VMValue[0];
        VMValue[] newArgs = new VMValue[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        return newArgs;
    }

    private @NotNull VMValue accessConstructor(@NotNull VMFrame frame, @NotNull VMClass caller, @NotNull VMObject instance, @NotNull VMValue[] args)
    {
        VMObject initMethod = (VMObject) instance.getField("initMethod");
        VMClassObject instanceClass = (VMClassObject) instance.getField("instanceClass");
        VMResolvedMethodName resolved = (VMResolvedMethodName) initMethod.getField("method");

        VMMethod method = resolved.getMethod();
        VMValue[] invokeArgs = obtainArguments(args, method);

        VMClass thisClass = instanceClass.getRepresentingClass();
        VMObject newInstance = thisClass.createInstance();

        newInstance.initialiseInstance(
                frame,
                caller,
                method,
                invokeArgs,
                true
        );

        return newInstance;
    }

    private void setResultCache(@NotNull VMObject instance, @NotNull VMValue result)
    {
        this.resultCache.put(instance.identityHashCode(), result);
    }

    private VMValue checkCache(@NotNull VMObject instance)
    {
        return this.resultCache.remove(instance.identityHashCode());
    }
}
