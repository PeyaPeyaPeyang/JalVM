package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMStringCreator;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;

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
                        return new VMInteger(16);
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
                        VMClass type = clazzObject.getClazz();
                        if (type == null)
                            return new VMInteger(8);  // 多分 Object
                        String descriptor = type.getTypeDescriptor();
                        if (descriptor.startsWith("L"))
                            return new VMInteger(8);
                        switch (descriptor.charAt(0)) {
                            case 'Z', 'B' -> {
                                return new VMInteger(1);
                            }
                            case 'C', 'S' -> {
                                return new VMInteger(2);
                            }
                            case 'I', 'F' -> {
                                return new VMInteger(4);
                            }
                            case 'J', 'D' -> {
                                return new VMInteger(8);
                            }
                        }
                        return new VMInteger(4); // Default to 4 for unknown types
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
                        VMObject fieldName = (VMObject) args[1];
                        String fieldNameStr = VMStringCreator.getString(fieldName);
                        VMField field = clazzObject.getTypeOf().getLinkedClass().findField(fieldNameStr);
                        return new VMLong(field.getFieldID());
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
                        VMInteger convertedCurrentValue = (VMInteger) currentValue.conformValue(VMType.INTEGER);
                        int currentIntValue = convertedCurrentValue.asNumber().intValue();
                        boolean success = currentIntValue == expected;
                        if (success)
                            object.setField(field, new VMInteger(newValue));

                        return VMBoolean.of(success);
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
                        VMInteger convertedCurrentValue = (VMInteger) currentValue.conformValue(VMType.INTEGER);
                        int currentIntValue = convertedCurrentValue.asNumber().intValue();
                        boolean success = currentIntValue == expected;
                        if (success)
                            object.setField(field, new VMInteger(newValue));

                        return new VMInteger(success ? currentIntValue : expected);
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
                        VMLong convertedCurrentValue = (VMLong) currentValue.conformValue(VMType.LONG);
                        long currentIntValue = convertedCurrentValue.asNumber().longValue();
                        boolean success = currentIntValue == expected;
                        if (success)
                            object.setField(field, new VMLong(newValue));

                        return VMBoolean.of(success);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "compareAndExchangeLong",
                        "(Ljava/lang/Object;JJJ)I",
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
                        VMLong convertedCurrentValue = (VMLong) currentValue.conformValue(VMType.LONG);
                        long currentIntValue = convertedCurrentValue.asNumber().longValue();
                        boolean success = currentIntValue == expected;
                        if (success)
                            object.setField(field, new VMLong(newValue));

                        return new VMLong(success ? currentIntValue : expected);
                    }
                }
        );
    }

}
