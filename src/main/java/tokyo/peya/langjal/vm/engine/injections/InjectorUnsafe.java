package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

public class InjectorUnsafe implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("jdk/internal/misc/Unsafe");

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "registerNatives",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "arrayBaseOffset0",
                        "(Ljava/lang/Class;)I",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return new VMInteger(thread, getArrayBaseOffset());
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "arrayIndexScale0",
                        "(Ljava/lang/Class;)I",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject clazzObject = (VMClassObject) args[0];
                        int scale = getArrayScale(clazzObject.getRepresentingClass());
                        return new VMInteger(thread, scale);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "objectFieldOffset1",
                        "(Ljava/lang/Class;Ljava/lang/String;)J",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject clazzObject = (VMClassObject) args[0];
                        VMStringObject fieldNameObject = (VMStringObject) args[1];
                        String fieldNameStr = fieldNameObject.getString();
                        VMField field = clazzObject.getTypeOf().getLinkedClass().findField(fieldNameStr);
                        return new VMLong(thread, field.getFieldID());
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "fullFence",
                        "()V",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "compareAndSetReference",
                        "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMReferenceValue object = (VMReferenceValue) args[0];
                        long offset = ((VMLong) args[1]).asNumber().longValue();
                        VMReferenceValue expected = (VMReferenceValue) args[2];
                        VMReferenceValue newValue = (VMReferenceValue) args[3];

                        boolean success;
                        if (object instanceof VMArray array)
                        {
                            // 配列の場合は、配列の要素を取得
                            int index = (int) (offset - getArrayBaseOffset()) / getArrayScale(array.getObjectType());
                            if (index < 0 || index >= array.length())
                                throw new VMPanic("Array index out of bounds: " + index);
                            VMValue value = array.get(index);
                            success = value.equals(expected);
                            if (success)
                                array.set(index, newValue);
                        }
                        else if (object instanceof VMObject obj)
                        {
                            VMField field = obj.getObjectType().findField(offset);
                            VMValue currentValue = obj.getField(field.getName());
                            success = currentValue.equals(expected);
                            if (success)
                                obj.setField(field, newValue);
                        }
                        else
                            throw new VMPanic("Unsupported object type for compareAndSet: " + object.getClass().getName());

                        return VMBoolean.of(thread, success);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "compareAndExchangeReference",
                        "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject object = (VMObject) args[0];
                        long offset = ((VMLong) args[1]).asNumber().longValue();
                        VMReferenceValue expected = (VMReferenceValue) args[2];
                        VMReferenceValue newValue = (VMReferenceValue) args[3];
                        VMField field = object.getObjectType().findField(offset);
                        VMValue currentValue = object.getField(field.getName());
                        boolean success = currentValue.equals(expected);
                        if (success)
                            object.setField(field, newValue);
                        return success ? currentValue : expected;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "compareAndSetInt",
                        "(Ljava/lang/Object;JII)Z",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject object = (VMObject) args[0];
                        long offset = ((VMLong) args[1]).asNumber().longValue();
                        int expected = ((VMInteger) args[2]).asNumber().intValue();
                        int newValue = ((VMInteger) args[3]).asNumber().intValue();

                        VMField field = object.getObjectType().findField(offset);
                        VMValue currentValue = object.getField(field.getName());
                        VMInteger convertedCurrentValue = (VMInteger) currentValue.conformValue(VMType.of(thread, PrimitiveTypes.INT));
                        int currentIntValue = convertedCurrentValue.asNumber().intValue();
                        boolean success = currentIntValue == expected;
                        if (success)
                            object.setField(field, new VMInteger(thread, newValue));

                        return VMBoolean.of(thread, success);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "compareAndExchangeInt",
                        "(Ljava/lang/Object;JII)I",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject object = (VMObject) args[0];
                        long offset = ((VMLong) args[1]).asNumber().longValue();
                        int expected = ((VMInteger) args[2]).asNumber().intValue();
                        int newValue = ((VMInteger) args[3]).asNumber().intValue();

                        VMField field = object.getObjectType().findField(offset);
                        VMValue currentValue = object.getField(field.getName());
                        VMInteger convertedCurrentValue = (VMInteger) currentValue.conformValue(VMType.of(thread, PrimitiveTypes.INT));
                        int currentIntValue = convertedCurrentValue.asNumber().intValue();
                        boolean success = currentIntValue == expected;
                        if (success)
                            object.setField(field, new VMInteger(thread, newValue));

                        return new VMInteger(thread, success ? currentIntValue : expected);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "compareAndSetLong",
                        "(Ljava/lang/Object;JJJ)Z",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject object = (VMObject) args[0];
                        long offset = ((VMLong) args[1]).asNumber().longValue();
                        long expected = ((VMLong) args[2]).asNumber().longValue();
                        long newValue = ((VMLong) args[3]).asNumber().longValue();

                        VMField field = object.getObjectType().findField(offset);
                        VMValue currentValue = object.getField(field.getName());
                        VMLong convertedCurrentValue = (VMLong) currentValue.conformValue(VMType.of(thread, PrimitiveTypes.LONG));
                        long currentIntValue = convertedCurrentValue.asNumber().longValue();
                        boolean success = currentIntValue == expected;
                        if (success)
                            object.setField(field, new VMLong(thread, newValue));

                        return VMBoolean.of(thread, success);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "compareAndExchangeLong",
                        "(Ljava/lang/Object;JJJ)J",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject object = (VMObject) args[0];
                        long offset = ((VMLong) args[1]).asNumber().longValue();
                        long expected = ((VMLong) args[2]).asNumber().longValue();
                        long newValue = ((VMLong) args[3]).asNumber().longValue();

                        VMField field = object.getObjectType().findField(offset);
                        VMValue currentValue = object.getField(field.getName());
                        VMLong convertedCurrentValue = (VMLong) currentValue.conformValue(VMType.of(thread, PrimitiveTypes.LONG));
                        long currentIntValue = convertedCurrentValue.asNumber().longValue();
                        boolean success = currentIntValue == expected;
                        if (success)
                            object.setField(field, new VMLong(thread, newValue));

                        return new VMLong(thread, success ? currentIntValue : expected);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "getReferenceVolatile",
                        "(Ljava/lang/Object;J)Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject object = (VMObject) args[0];
                        long offset = ((VMLong) args[1]).asNumber().longValue();
                        VMField field = object.getObjectType().findField(offset);
                        return object.getField(field.getName());
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "putReferenceVolatile",
                        "(Ljava/lang/Object;JLjava/lang/Object;)V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject object = (VMObject) args[0];
                        long offset = ((VMLong) args[1]).asNumber().longValue();
                        VMObject value = (VMObject) args[2];
                        VMField field = object.getObjectType().findField(offset);
                        object.setField(field, value);
                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "getInt",
                        "(Ljava/lang/Object;J)I",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject object = (VMObject) args[0];
                        long offset = ((VMLong) args[1]).asNumber().longValue();
                        VMField field = object.getObjectType().findField(offset);
                        VMValue value = object.getField(field.getName());
                        return value.conformValue(VMType.of(thread, PrimitiveTypes.INT));
                    }
                }
        );

        JalVM vm = cl.getVm();
        clazz.injectMethod(cl, createUnsafeGetVolatileMethod(
                clazz, "getReferenceVolatile", "(Ljava/lang/Object;J)Ljava/lang/Object;", VMType.ofGenericObject(vm)
        ));
        clazz.injectMethod(cl, createUnsafePutVolatileMethod(
                clazz, "putReferenceVolatile", "(Ljava/lang/Object;JLjava/lang/Object;)V", VMType.ofGenericObject(vm)
        ));
        clazz.injectMethod(cl, createUnsafeGetVolatileMethod(
                clazz, "getIntVolatile", "(Ljava/lang/Object;J)I", VMType.of(vm, PrimitiveTypes.INT)
        ));
        clazz.injectMethod(cl, createUnsafePutVolatileMethod(
                        clazz, "putIntVolatile", "(Ljava/lang/Object;JI)V", VMType.of(vm, PrimitiveTypes.INT)
        ));
        clazz.injectMethod(cl, createUnsafeGetVolatileMethod(
                clazz, "getBooleanVolatile", "(Ljava/lang/Object;J)Z", VMType.of(vm, PrimitiveTypes.BOOLEAN)
        ));
        clazz.injectMethod(cl, createUnsafePutVolatileMethod(
                clazz, "putBooleanVolatile", "(Ljava/lang/Object;JZ)V", VMType.of(vm, PrimitiveTypes.BOOLEAN)
        ));
        clazz.injectMethod(cl, createUnsafeGetVolatileMethod(
                clazz, "getByteVolatile", "(Ljava/lang/Object;J)B", VMType.of(vm, PrimitiveTypes.BYTE)
        ));
        clazz.injectMethod(cl, createUnsafePutVolatileMethod(
                clazz, "putByteVolatile", "(Ljava/lang/Object;JB)V", VMType.of(vm, PrimitiveTypes.BYTE)
        ));
        clazz.injectMethod(cl, createUnsafeGetVolatileMethod(
                clazz, "getShortVolatile", "(Ljava/lang/Object;J)S", VMType.of(vm, PrimitiveTypes.SHORT)
        ));
        clazz.injectMethod(cl, createUnsafePutVolatileMethod(
                clazz, "putShortVolatile", "(Ljava/lang/Object;JS)V", VMType.of(vm, PrimitiveTypes.SHORT)
        ));
        clazz.injectMethod(cl, createUnsafeGetVolatileMethod(
                clazz, "getCharVolatile", "(Ljava/lang/Object;J)C", VMType.of(vm, PrimitiveTypes.CHAR)
        ));
        clazz.injectMethod(cl, createUnsafePutVolatileMethod(
                clazz, "putCharVolatile", "(Ljava/lang/Object;JC)V", VMType.of(vm, PrimitiveTypes.CHAR)
        ));
        clazz.injectMethod(cl, createUnsafeGetVolatileMethod(
                clazz, "getLongVolatile", "(Ljava/lang/Object;J)J", VMType.of(vm, PrimitiveTypes.LONG)
        ));
        clazz.injectMethod(cl, createUnsafePutVolatileMethod(
                clazz, "putLongVolatile", "(Ljava/lang/Object;JJ)V", VMType.of(vm, PrimitiveTypes.LONG)
        ));
        clazz.injectMethod(cl, createUnsafeGetVolatileMethod(
                clazz, "getFloatVolatile", "(Ljava/lang/Object;J)F", VMType.of(vm, PrimitiveTypes.FLOAT)
        ));
        clazz.injectMethod(cl, createUnsafePutVolatileMethod(
                clazz, "putFloatVolatile", "(Ljava/lang/Object;JF)V", VMType.of(vm, PrimitiveTypes.FLOAT)
        ));
        clazz.injectMethod(cl, createUnsafeGetVolatileMethod(
                clazz, "getDoubleVolatile", "(Ljava/lang/Object;J)D", VMType.of(vm, PrimitiveTypes.DOUBLE)
        ));
        clazz.injectMethod(cl, createUnsafePutVolatileMethod(
                clazz, "putDoubleVolatile", "(Ljava/lang/Object;JD)V", VMType.of(vm, PrimitiveTypes.DOUBLE)
        ));

        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "shouldBeInitialized0",
                        "(Ljava/lang/Class;)Z",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject clazzObject = (VMClassObject) args[0];
                        return VMBoolean.of(thread, !clazzObject.getRepresentingClass().isInitialised());
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "ensureClassInitialized0",
                        "(Ljava/lang/Class;)V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject clazzObject = (VMClassObject) args[0];
                        VMClass repClass = clazzObject.getRepresentingClass();
                        if (!repClass.isInitialised())
                            repClass.initialise(thread);
                        return null;
                    }
                }
        );
    }

    private static InjectedMethod createUnsafeGetVolatileMethod(
            @NotNull VMClass clazz, @NotNull String methodName, @NotNull String descriptor, @NotNull VMType<?> returnType
    )
    {
        return new InjectedMethod(
                clazz, new MethodNode(
                EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                methodName,
                descriptor,
                null,
                null
        )
        )
        {
            @Override
            public @Nullable VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                            @Nullable VMObject instance, @NotNull VMValue[] args)
            {
                VMReferenceValue object = (VMReferenceValue) args[0];
                long offset = ((VMLong) args[1]).asNumber().longValue();
                if (object instanceof VMArray array)
                {
                    // 配列の場合は、配列の要素を取得
                    int index = (int) (offset - getArrayBaseOffset()) / getArrayScale(array.getObjectType());
                    if (index < 0 || index >= array.length())
                        throw new VMPanic("Array index out of bounds: " + index);
                    VMValue value = array.get(index);
                    return value.conformValue(returnType);
                }
                else if (object instanceof VMObject vmObject)
                {
                    VMField field = vmObject.getObjectType().findField(offset);
                    VMValue value = vmObject.getField(field.getName());
                    return value.conformValue(returnType);
                }

                throw new VMPanic("Unsupported object type for getVolatile: " + object.getClass().getName());
            }
        };
    }

    public static InjectedMethod createUnsafePutVolatileMethod(
            @NotNull VMClass clazz, @NotNull String methodName, @NotNull String descriptor, @NotNull VMType<?> valueType
    )
    {
        return new InjectedMethod(
                clazz, new MethodNode(
                EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                methodName,
                descriptor,
                null,
                null
        )
        )
        {
            @Override
            public @Nullable VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                            @Nullable VMObject instance, @NotNull VMValue[] args)
            {
                VMObject object = (VMObject) args[0];
                long offset = ((VMLong) args[1]).asNumber().longValue();
                VMValue value = args[2].conformValue(valueType);
                VMField field = object.getObjectType().findField(offset);
                object.setField(field, value);
                return null;
            }
        };
    }

    private static int getArrayBaseOffset()
    {
        return 16;  // とりあえず
    }

    private static int getArrayScale(VMType<?> clazz)
    {
        if (clazz == null)
            return 8; // Object と同等としてデフォルト

        String descriptor = clazz.getTypeDescriptor();
        if (descriptor.startsWith("L"))
            return 8; // Object 型

        return switch (descriptor.charAt(0))
        {
            case 'Z', 'B' -> 1;
            case 'C', 'S' -> 2;
            case 'I', 'F' -> 4;
            case 'J', 'D' -> 8;
            default -> 4; // その他はとりあえず 4
        };
    }
}
