package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMListAccessor;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.invoke.VMResolvedMethodName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InjectorMethodHandle implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/invoke/MethodHandle");

    private final Map<Integer, VMValue> resultCache;

    private /* final */ VMClass fieldAccessor;
    private /* final */ VMClass fieldAccessorStatic;
    private /* final */ VMClass constructorAccessor;
    private /* final */ VMClass boundMethodHandle;
    private /* final */ VMClass speciesData;

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
        this.speciesData = cl.findClass(ClassReference.of("java/lang/invoke/BoundMethodHandle$SpeciesData"));

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
                        return InjectorMethodHandle.this.invoke(frame, caller, instance, args);
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
                        return InjectorMethodHandle.this.invoke(frame, caller, instance, args);
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
        else if (this.fieldAccessorStatic.isInstance(instance))
            return accessStaticField(frame, instance);
        else if (this.constructorAccessor.isInstance(instance))
            return this.accessConstructor(frame, caller, instance, args);

        return this.accessLambdaForm(frame, instance, args);
    }

    private VMValue invokeFieldGetter(@NotNull VMComponent com, @NotNull VMObject getter, @Nullable VMObject instance)
    {
        if (this.fieldAccessorStatic.isInstance(getter))
            return accessStaticField(com, getter);
        else if (this.fieldAccessor.isInstance(getter))
            return accessInstanceField(getter, instance);
        else
            throw new VMPanic("Not a field accessor: " + getter.getObjectType().getReference().toString());
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
            throw new NullPointerException("Static field not found by ID: " + staticOffset + " (field type: " + fieldType.getReference().toString() + ")");

        return field.getClazz().getStaticFieldValue(field);
    }

    private @Nullable VMValue accessLambdaForm(@NotNull VMFrame frame, @NotNull VMObject methodHandle, @NotNull VMValue[] args)
    {
        VMValue cached = this.checkCache(methodHandle);
        if (cached != null)
            return cached;

        VMObject lambdaForm = (VMObject) methodHandle.getField("form");

        VMObject vmEntry  = (VMObject) lambdaForm.getField("vmentry");
        VMResolvedMethodName resolved = (VMResolvedMethodName) vmEntry.getField("method");

        VMMethod method = resolved.getMethod();
        VMValue[] invokeArgs = this.obtainLambdaArguments(lambdaForm, method, methodHandle, args);

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
                v -> this.setResultCache(methodHandle, v),
                thisObject,
                invokeArgs
        );

        return null;
    }

    private VMValue[] obtainLambdaArguments(@NotNull VMObject lambdaForm,
                                            @NotNull VMMethod method,
                                            @NotNull VMObject methodHandle,
                                            @NotNull VMValue[] args)
    {
        /*
        // BoundMethodHandle ならば，あらかじめ構成されている引数を取り出す
        VMArray names = (VMArray) lambdaForm.getField("names");  // LambdaForm$Name[]
        assert names.length() > 0;
        VMObject firstName = (VMObject) names.get(0);  // LambdaForm$Name
        VMObject constraint = (VMObject) firstName.getField("constraint");  // Object
        if (this.speciesData.isInstance(constraint))
        {
            JalVM vm = method.getClazz().getVM();
            VMObject getters = (VMObject) constraint.getField("getters");  // List<LambdaForm$Name>
            VMValue[] getterValues = VMListAccessor.values(getters);
            VMValue[] boundArgs = new VMValue[getterValues.length];
            for (int i = 0; i < getterValues.length; i++)
            {
                VMObject getter = (VMObject) getterValues[i];
                VMValue getterResult = this.invokeFieldGetter(vm, getter, methodHandle);
                boundArgs[i] = getterResult;
            }

            // 引数リストを結合する
            VMValue[] newArgs = new VMValue[boundArgs.length + args.length];
            System.arraycopy(boundArgs, 0, newArgs, 0, boundArgs.length);
            System.arraycopy(args, 0, newArgs, boundArgs.length, args.length);
            args = newArgs;
        }*/

        VMValue[] newArgs = new VMValue[1 + args.length];
        newArgs[0] = methodHandle;
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return this.obtainArguments(method, newArgs);
    }

    private VMValue[] obtainArguments(@NotNull VMMethod method,
                                      @NotNull VMValue[] args)
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
        VMValue[] invokeArgs = this.obtainArguments(method, args);

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
