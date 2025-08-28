package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.exceptions.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMLong;
import tokyo.peya.langjal.vm.values.VMNull;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMConstructorObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMFieldObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMMethodObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.invoke.VMResolvedMethodName;

import java.util.List;

public class InjectorMethodHandleNatives implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/invoke/MethodHandleNatives");

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        VMClass constantsClass = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandleNatives$Constants"));

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
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "getNamedCon",
                        "(I[Ljava/lang/Object;)I",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        if (!constantsClass.isInitialised())
                            constantsClass.initialise(thread);
                        // 本来は init されていなかったら，初期化する必要がある。
                        // しかし，定数を取るだけならそのままでも良い。

                        int which = ((VMInteger) args[0]).asNumber().intValue();
                        VMArray arr = (VMArray) args[1];

                        VMField field = constantsClass.getFields()
                                .stream()
                                .filter(f -> f.getAccessAttributes().has(AccessAttribute.STATIC) && f.getAccessAttributes().has(AccessAttribute.FINAL))
                                .skip(which)
                                .findFirst()
                                .orElse(null);
                        if (field == null)
                        {
                            // 終端記号
                            arr.set(0, new VMNull<>(VMType.GENERIC_OBJECT));
                            return VMInteger.M1;
                        }

                        VMValue value = constantsClass.getStaticFieldValue(field);
                        String fieldName = field.getName();
                        arr.set(0, VMStringObject.createString(thread, fieldName));

                        return value;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "init",
                        "(Ljava/lang/invoke/MemberName;Ljava/lang/Object;)V",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject memberName = (VMObject) args[0];
                        VMObject lookup = (VMObject) args[1];

                        VMInteger modifiers = (VMInteger) lookup.getField("modifiers");

                        int flags;
                        int modifiersInt = modifiers.asNumber().intValue();
                        switch (lookup)
                        {
                            case VMFieldObject _ -> {
                                flags = calcFieldFlags(modifiersInt);
                                memberName.setField("name", lookup.getField("name"));
                                memberName.setField("type", lookup.getField("type"));
                            }
                            case VMConstructorObject constructor -> flags = calcMethodFlags(modifiersInt, true);
                            case VMMethodObject method -> flags = calcMethodFlags(modifiersInt, false);
                            default -> flags = 0;
                        }

                        memberName.setField("flags", new VMInteger(flags));
                        memberName.setField("clazz", lookup.getField("clazz"));

                        return null;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "resolve",
                        "(Ljava/lang/invoke/MemberName;Ljava/lang/Class;IZ)Ljava/lang/invoke/MemberName;",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject memberName = (VMObject) args[0];
                        VMClass clazz = ((VMClassObject) memberName.getField("clazz")).getRepresentingClass();
                        String methodName = ((VMStringObject) memberName.getField("name")).getString();

                        VMMethod m = clazz.findMethod(methodName, null);
                        if (m == null)
                            throw new VMPanic("Cannot resolve method: " + clazz + "->" + methodName);

                        memberName.setField("method", new VMResolvedMethodName(cl, m));

                        return memberName;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "getMemberVMInfo",
                        "(Ljava/lang/invoke/MemberName;)Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    @Override VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject memberName = (VMObject) args[0];
                        VMArray arr = new VMArray(thread.getVm().getClassLoader(), VMType.GENERIC_OBJECT, 2);
                        VMClassObject clazzObj = (VMClassObject) memberName.getField("clazz");
                        VMClass clazz = clazzObj.getRepresentingClass();

                        VMReferenceValue method = (VMReferenceValue) memberName.getField("method");
                        if (method instanceof VMResolvedMethodName resolved)
                            arr.set(0, new VMLong(resolved.getMethod().getSlot()));


                        arr.set(1, clazzObj);

                        return arr;
                    }
                }
        );
    }

    private static int calcMethodFlags(int modifier, boolean isConstructor)
    {
        boolean isStatic = (EOpcodes.ACC_STATIC & modifier) != 0;
        return calcFlags(
                isConstructor ? 0x00020000 : 0x00010000,
                modifier,
                isStatic ? /* REF_getStatic */ 2 : /* REF_getField */ 1
        );
    }

    private static int calcFieldFlags(int modifier)
    {
        boolean isStatic = (EOpcodes.ACC_STATIC & modifier) != 0;
        return calcFlags(0x00040000, modifier, isStatic ? /* REF_getStatic */ 2 : /* REF_getField */ 1);
    }

    private static int calcFlags(int is, int modifier, int refKind)
    {
        return is | modifier | (refKind << 24);
    }
}
