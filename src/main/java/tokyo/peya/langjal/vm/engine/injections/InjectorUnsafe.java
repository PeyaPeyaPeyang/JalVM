package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.PrimitiveTypes;
import tokyo.peya.langjal.vm.JalVM;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMComponent;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMNull;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMPrimitive;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

import java.awt.Frame;

public class InjectorUnsafe implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("jdk/internal/misc/Unsafe");

    @Override
    public @NotNull ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectMethod(
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
                    @Nullable VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null;
                    }
                }
        );
        clazz.injectMethod(
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
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return new VMInteger(frame, getArrayBaseOffset());
                    }
                }
        );
        clazz.injectMethod(
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
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject clazzObject = (VMClassObject) args[0];
                        int scale = getArrayScale(clazzObject.getRepresentingClass());
                        return new VMInteger(frame, scale);
                    }
                }
        );
        clazz.injectMethod(
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
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject clazzObject = (VMClassObject) args[0];
                        VMStringObject fieldNameObject = (VMStringObject) args[1];
                        String fieldNameStr = fieldNameObject.getString();
                        VMField field = clazzObject.getTypeOf().getLinkedClass().findField(fieldNameStr);
                        return new VMLong(frame, field.getFieldID());
                    }
                }
        );
        clazz.injectMethod(
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
                    @Nullable VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null;
                    }
                }
        );

        VMType<VMReferenceValue> genericObjectType = VMType.ofGenericObject(cl.getVM());
        CompareAndSetStrategy<VMReferenceValue> referenceStrategy = Object::equals;

        clazz.injectMethod(InjectorUnsafe.makeInjectedCASSetMethod(
                clazz,
                "compareAndSetReference",
                "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z",
                genericObjectType,
                referenceStrategy
        ));
        clazz.injectMethod(InjectorUnsafe.makeInjectedCASExchangeMethod(
                clazz,
                "compareAndExchangeReference",
                "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                genericObjectType,
                referenceStrategy
        ));

        VMType<VMInteger> intType = VMType.of(cl, PrimitiveTypes.INT);
        CompareAndSetStrategy<VMInteger> intStrategy = (current, expected) -> {
            if (!(current instanceof VMInteger currentInt && expected instanceof VMInteger expectedInt))
                return false;

            return currentInt.asNumber().intValue() == expectedInt.asNumber().intValue();
        };

        clazz.injectMethod(InjectorUnsafe.makeInjectedCASSetMethod(
                clazz,
                "compareAndSetInt",
                "(Ljava/lang/Object;JII)Z",
                intType,
                intStrategy
        ));
        clazz.injectMethod(InjectorUnsafe.makeInjectedCASExchangeMethod(
                clazz,
                "compareAndExchangeInt",
                "(Ljava/lang/Object;JII)I",
                intType,
                intStrategy
        ));

        VMType<VMLong> longType = VMType.of(cl, PrimitiveTypes.LONG);
        CompareAndSetStrategy<VMLong> longStrategy = (current, expected) -> {
            if (!(current instanceof VMLong currentLong && expected instanceof VMLong expectedLong))
                return false;

            return currentLong.asNumber().longValue() == expectedLong.asNumber().longValue();
        };

        clazz.injectMethod(InjectorUnsafe.makeInjectedCASSetMethod(
                clazz,
                "compareAndSetLong",
                "(Ljava/lang/Object;JJJ)Z",
                longType,
                longStrategy
        ));

        clazz.injectMethod(InjectorUnsafe.makeInjectedCASExchangeMethod(
                clazz,
                "compareAndExchangeLong",
                "(Ljava/lang/Object;JJJ)J",
                longType,
                longStrategy
        ));

        clazz.injectMethod(
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
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject object = (VMObject) args[0];
                        long offset = ((VMLong) args[1]).asNumber().longValue();
                        VMField field = object.getObjectType().findField(offset);
                        VMValue value = object.getField(field.getName());
                        return value.conformValue(VMType.of(frame, PrimitiveTypes.INT));
                    }
                }
        );

        JalVM vm = cl.getVM();
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getReferenceVolatile", "(Ljava/lang/Object;J)Ljava/lang/Object;", VMType.ofGenericObject(vm)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putReferenceVolatile", "(Ljava/lang/Object;JLjava/lang/Object;)V", VMType.ofGenericObject(vm)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getIntVolatile", "(Ljava/lang/Object;J)I", VMType.of(vm, PrimitiveTypes.INT)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putIntVolatile", "(Ljava/lang/Object;JI)V", VMType.of(vm, PrimitiveTypes.INT)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getBooleanVolatile", "(Ljava/lang/Object;J)Z", VMType.of(vm, PrimitiveTypes.BOOLEAN)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putBooleanVolatile", "(Ljava/lang/Object;JZ)V", VMType.of(vm, PrimitiveTypes.BOOLEAN)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getByteVolatile", "(Ljava/lang/Object;J)B", VMType.of(vm, PrimitiveTypes.BYTE)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putByteVolatile", "(Ljava/lang/Object;JB)V", VMType.of(vm, PrimitiveTypes.BYTE)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getShortVolatile", "(Ljava/lang/Object;J)S", VMType.of(vm, PrimitiveTypes.SHORT)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putShortVolatile", "(Ljava/lang/Object;JS)V", VMType.of(vm, PrimitiveTypes.SHORT)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getCharVolatile", "(Ljava/lang/Object;J)C", VMType.of(vm, PrimitiveTypes.CHAR)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putCharVolatile", "(Ljava/lang/Object;JC)V", VMType.of(vm, PrimitiveTypes.CHAR)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getLongVolatile", "(Ljava/lang/Object;J)J", VMType.of(vm, PrimitiveTypes.LONG)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putLongVolatile", "(Ljava/lang/Object;JJ)V", VMType.of(vm, PrimitiveTypes.LONG)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getFloatVolatile", "(Ljava/lang/Object;J)F", VMType.of(vm, PrimitiveTypes.FLOAT)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putFloatVolatile", "(Ljava/lang/Object;JF)V", VMType.of(vm, PrimitiveTypes.FLOAT)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getDoubleVolatile", "(Ljava/lang/Object;J)D", VMType.of(vm, PrimitiveTypes.DOUBLE)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putDoubleVolatile", "(Ljava/lang/Object;JD)V", VMType.of(vm, PrimitiveTypes.DOUBLE)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getReference", "(Ljava/lang/Object;J)Ljava/lang/Object;", VMType.ofGenericObject(vm)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putReference", "(Ljava/lang/Object;JLjava/lang/Object;)V", VMType.ofGenericObject(vm)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getInt", "(Ljava/lang/Object;J)I", VMType.of(vm, PrimitiveTypes.INT)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putInt", "(Ljava/lang/Object;JI)V", VMType.of(vm, PrimitiveTypes.INT)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getBoolean", "(Ljava/lang/Object;J)Z", VMType.of(vm, PrimitiveTypes.BOOLEAN)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putBoolean", "(Ljava/lang/Object;JZ)V", VMType.of(vm, PrimitiveTypes.BOOLEAN)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getByte", "(Ljava/lang/Object;J)B", VMType.of(vm, PrimitiveTypes.BYTE)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putByte", "(Ljava/lang/Object;JB)V", VMType.of(vm, PrimitiveTypes.BYTE)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getShort", "(Ljava/lang/Object;J)S", VMType.of(vm, PrimitiveTypes.SHORT)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putShort", "(Ljava/lang/Object;JS)V", VMType.of(vm, PrimitiveTypes.SHORT)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getChar", "(Ljava/lang/Object;J)C", VMType.of(vm, PrimitiveTypes.CHAR)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putChar", "(Ljava/lang/Object;JC)V", VMType.of(vm, PrimitiveTypes.CHAR)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getLong", "(Ljava/lang/Object;J)J", VMType.of(vm, PrimitiveTypes.LONG)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putLong", "(Ljava/lang/Object;JJ)V", VMType.of(vm, PrimitiveTypes.LONG)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getFloat", "(Ljava/lang/Object;J)F", VMType.of(vm, PrimitiveTypes.FLOAT)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putFloat", "(Ljava/lang/Object;JF)V", VMType.of(vm, PrimitiveTypes.FLOAT)
        ));
        clazz.injectMethod(createUnsafeReferenceGetMethod(
                clazz, "getDouble", "(Ljava/lang/Object;J)D", VMType.of(vm, PrimitiveTypes.DOUBLE)
        ));
        clazz.injectMethod(createUnsafeReferencePutMethod(
                clazz, "putDouble", "(Ljava/lang/Object;JD)V", VMType.of(vm, PrimitiveTypes.DOUBLE)
        ));

        clazz.injectMethod(
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
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject clazzObject = (VMClassObject) args[0];
                        return VMBoolean.of(frame, !clazzObject.getRepresentingClass().isInitialised());
                    }
                }
        );
        clazz.injectMethod(
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
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject clazzObject = (VMClassObject) args[0];
                        VMClass repClass = clazzObject.getRepresentingClass();
                        if (!repClass.isInitialised())
                            repClass.initialise(frame.getThread());
                        return null;
                    }
                }
        );
    }

    private static InjectedMethod createUnsafeReferenceGetMethod(
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
            public @Nullable VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                            @Nullable VMObject instance, @NotNull VMValue[] args)
            {
                VMReferenceValue object = (VMReferenceValue) args[0];
                long offset = ((VMLong) args[1]).asNumber().longValue();
                switch (object)
                {
                    case VMArray array ->
                    {
                        return read(
                                frame,
                                array,
                                offset - getArrayBaseOffset(),
                                getArrayScale(returnType)
                        );
                    }
                    case VMObject vmObject ->
                    {
                        VMField field = vmObject.getObjectType().findField(offset);
                        VMValue value = vmObject.getField(field.getName());
                        return value.conformValue(returnType);
                    }
                    case VMNull<?> _ ->
                    {
                        VMField field = frame.getVM().getHeap().getStaticFieldByID(offset);
                        if (field == null)
                            throw new VMPanic("No static field with ID: " + offset);
                        VMValue value = field.getOwningClass().getStaticFieldValue(field);
                        return value.conformValue(returnType);
                    }
                    default ->
                    {
                    }
                }

                throw new VMPanic("Unsupported object type for getVolatile: " + object.getClass().getName());
            }
        };
    }

    public static InjectedMethod createUnsafeReferencePutMethod(
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
            public @Nullable VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                            @Nullable VMObject instance, @NotNull VMValue[] args)
            {
                VMReferenceValue object = (VMReferenceValue) args[0];
                long offset = ((VMLong) args[1]).asNumber().longValue();
                VMValue value = args[2].conformValue(valueType);
                switch (object)
                {
                    case VMArray array ->
                    {
                        int index = (int) (offset - getArrayBaseOffset()) / getArrayScale(array.getElementType());
                        if (index < 0 || index >= array.length())
                            throw new VMPanic("Array index out of bounds: " + index);
                        array.set(index, value);
                    }
                    case VMObject vmObject ->
                    {
                        VMField field = vmObject.getObjectType().findField(offset);
                        vmObject.setField(field, value);
                    }
                    case VMNull<?> vmNull ->
                    {
                        VMField field = frame.getVM().getHeap().getStaticFieldByID(offset);
                        if (field == null)
                            throw new VMPanic("No static field with ID: " + offset);
                        field.getOwningClass().setStaticField(field, value);
                    }
                    default ->
                    {
                    }
                }

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

    private static VMValue read(VMComponent com, @NotNull VMArray array, long offset, int size)
    {
        VMType<?> elementType = array.getElementType();
        if (!elementType.isPrimitive())
        {
            if (size != 8)
                throw new VMPanic("Unsupported size for reference type: " + size);

            int index = Math.toIntExact(offset / getArrayScale(elementType));
            return array.get(index);
        }

        VMValue[] elements = array.getElements();
        int elementSize = getArrayScale(elementType);
        int index = Math.toIntExact(offset / elementSize);

        switch (size)
        {
            case 1: // byte
                return new VMInteger(com, ((VMPrimitive) elements[index]).asNumber().intValue() & 0xFF);
            case 2: // char / short
            {
                int lo = ((VMPrimitive) elements[index]).asNumber().intValue() & 0xFF;
                int hi = ((VMPrimitive) elements[index + 1]).asNumber().intValue() & 0xFF;
                return new VMInteger(com, (hi << 8) | lo);
            }
            case 4: // int / float
            {
                int b0 = ((VMPrimitive) elements[index]).asNumber().intValue() & 0xFF;
                int b1 = ((VMPrimitive) elements[index + 1]).asNumber().intValue() & 0xFF;
                int b2 = ((VMPrimitive) elements[index + 2]).asNumber().intValue() & 0xFF;
                int b3 = ((VMPrimitive) elements[index + 3]).asNumber().intValue() & 0xFF;
                return new VMInteger(com, (b3 << 24) | (b2 << 16) | (b1 << 8) | b0);
            }
            case 8: // long / double
            {
                long val = 0;
                for (int i = 0; i < 8; i++)
                    val |= ((long)((VMPrimitive) elements[index + i]).asNumber().intValue() & 0xFF) << (8 * i);
                return new VMLong(com, val);
            }
            default:
                throw new VMPanic("Unsupported size: " + size);
        }
    }

    private static <T extends VMValue> InjectedMethod makeInjectedCASExchangeMethod(
            @NotNull VMClass clazz,
            @NotNull String methodName,
            @NotNull String methodDescriptor,
            @NotNull VMType<? extends T> valueType,
            @NotNull CompareAndSetStrategy<T> strategy
    ) {
        return new InjectedMethod(
                clazz,
                new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        methodName,
                        methodDescriptor,
                        null,
                        null
                )
        )
        {
            @Override
            @Nullable
            public VMValue invoke(
                    @NotNull VMFrame frame,
                    @Nullable VMClass caller,
                    @Nullable VMObject instance,
                    @NotNull VMValue[] args
            )
            {
                VMReferenceValue object = (VMReferenceValue) args[0];
                long offset = ((VMLong) args[1]).asNumber().longValue();
                VMValue expected = args[2];
                VMValue newValue = args[3];

                return compareAndExchange(clazz, object, offset, expected, newValue, strategy, valueType);
            }
        };
    }

    private static <T extends VMValue> InjectedMethod makeInjectedCASSetMethod(
            @NotNull VMClass clazz,
            @NotNull String methodName,
            @NotNull String methodDescriptor,
            @NotNull VMType<? extends T> valueType,
            @NotNull CompareAndSetStrategy<T> strategy
    ) {
        return new InjectedMethod(
                clazz,
                new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        methodName,
                        methodDescriptor,
                        null,
                        null
                )
        )
        {
            @Override
            @Nullable
            public VMValue invoke(
                    @NotNull VMFrame frame,
                    @Nullable VMClass caller,
                    @Nullable VMObject instance,
                    @NotNull VMValue[] args
            )
            {
                VMReferenceValue object = (VMReferenceValue) args[0];
                long offset = ((VMLong) args[1]).asNumber().longValue();
                VMValue expected = args[2];
                VMValue newValue = args[3];

                boolean success = compareAndSet(clazz, object, offset, expected, newValue, strategy, valueType);
                return VMBoolean.of(frame, success);
            }
        };
    }

    private static <T extends VMValue> boolean compareAndSet(
            @NotNull VMClass component,
            @NotNull VMReferenceValue object,
            long offset,
            @NotNull VMValue expected,
            @NotNull VMValue newValue,
            @NotNull CompareAndSetStrategy<T> strategy,
            @NotNull VMType<? extends T> valueType)
    {
        switch (object)
        {
            case VMArray array -> {
                int index = (int) (offset - getArrayBaseOffset()) / getArrayScale(array.getElementType());
                if (index < 0 || index >= array.length())
                    throw new VMPanic("Array index out of bounds: " + index);

                if (strategy.compare(array.get(index), expected))
                {
                    array.set(index, newValue.conformValue(valueType));
                    return true;
                }
            }
            case VMObject obj -> {
                VMField field = obj.getObjectType().findField(offset);
                VMValue current = obj.getField(field.getName());
                if (strategy.compare(current, expected))
                {
                    obj.setField(field, newValue.conformValue(valueType));
                    return true;
                }
            }
            case VMNull<?> _ -> {
                VMField field = component.getVM().getHeap().getStaticFieldByID(offset);
                if (field == null)
                    throw new VMPanic("No static field with ID: " + offset);
                VMValue current = field.getOwningClass().getStaticFieldValue(field);
                if (strategy.compare(current, expected))
                {
                    field.getOwningClass().setStaticField(field, newValue.conformValue(valueType));
                    return true;
                }
            }
            default -> throw new VMPanic("Unsupported object type: " + object.getClass().getName());
        }

        return false;
    }

    private static <T extends VMValue> VMValue compareAndExchange(
            @NotNull VMClass component,
            @NotNull VMReferenceValue object,
            long offset,
            @NotNull VMValue expected,
            @NotNull VMValue newValue,
            @NotNull CompareAndSetStrategy<T> strategy,
            @NotNull VMType<? extends T> valueType)
    {
        VMValue current;
        switch (object)
        {
            case VMArray array -> {
                int index = (int) (offset - getArrayBaseOffset()) / getArrayScale(array.getElementType());
                if (index < 0 || index >= array.length())
                    throw new VMPanic("Array index out of bounds: " + index);

                current = array.get(index);
                if (strategy.compare(current, expected))
                    array.set(index, newValue.conformValue(valueType));

            }
            case VMObject obj -> {
                VMField field = obj.getObjectType().findField(offset);
                current = obj.getField(field.getName());
                if (strategy.compare(current, expected))
                    obj.setField(field, newValue.conformValue(valueType));
            }
            case VMNull<?> _ -> {
                VMField field = component.getVM().getHeap().getStaticFieldByID(offset);
                if (field == null)
                    throw new VMPanic("No static field with ID: " + offset);

                current = field.getOwningClass().getStaticFieldValue(field);
                if (strategy.compare(current, expected))
                    field.getOwningClass().setStaticField(field, newValue.conformValue(valueType));
            }
            default -> throw new VMPanic("Unsupported object type: " + object.getClass().getName());
        }

        return current;
    }

    @FunctionalInterface
    private interface CompareAndSetStrategy<T extends VMValue>
    {
        boolean compare(VMValue current, @NotNull VMValue expected);
    }
}
