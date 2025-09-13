package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.compiler.jvm.MethodDescriptor;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
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

public class InjectorMethodHandleNatives implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/invoke/MethodHandleNatives");

    @Override
    public @NotNull ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        VMClass constantsClass = cl.findClass(ClassReference.of("java/lang/invoke/MethodHandleNatives$Constants"));

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
                    @Nullable
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return null;
                    }
                }
        );
        clazz.injectMethod(
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
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        if (!constantsClass.isInitialised())
                            constantsClass.initialise(frame.getThread());
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
                            arr.set(0, new VMNull<>(VMType.ofGenericObject(frame)));
                            return new VMInteger(frame, -1);
                        }

                        VMValue value = constantsClass.getStaticFieldValue(field);
                        String fieldName = field.getName();
                        arr.set(0, VMStringObject.createString(frame, fieldName));

                        return value;
                    }
                }
        );
        clazz.injectMethod(
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
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
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
                            case VMConstructorObject _ -> flags = calcMethodFlags(modifiersInt, true);
                            case VMMethodObject _ -> flags = calcMethodFlags(modifiersInt, false);
                            default -> flags = 0;
                        }

                        memberName.setField("flags", new VMInteger(frame, flags));
                        memberName.setField("clazz", lookup.getField("clazz"));

                        return null;
                    }
                }
        );
        clazz.injectMethod(
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
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject memberName = (VMObject) args[0];
                        VMClass clazz = ((VMClassObject) memberName.getField("clazz")).getRepresentingClass();

                        int flags = ((VMInteger) memberName.getField("flags")).asNumber().intValue();
                        if (isField(flags))
                        {
                            String fieldName = ((VMStringObject) memberName.getField("name")).getString();
                            VMField field = clazz.findFieldSafe(fieldName);
                            if (field == null)
                                return new VMNull<>(VMType.ofClassName(frame, "java/lang/invoke/MemberName"));
                            int access = field.getFieldNode().access;
                            flags = calcFieldFlags(access);
                            memberName.setField("flags", new VMInteger(frame, flags));
                            memberName.setField("type", field.getType().getLinkedClass().getClassObject());
                            return memberName;
                        }

                        String methodName = ((VMStringObject) memberName.getField("name")).getString();
                        VMObject methodType = (VMObject) memberName.getField("type");
                        MethodDescriptor descriptor = getDescriptorByMethodType(methodType);
                        if (descriptor == null)
                            return new VMNull<>(VMType.ofClassName(frame, "java/lang/invoke/MemberName"));

                        VMMethod m = clazz.findMethod(methodName, descriptor);
                        if (m == null)
                            return new VMNull<>(VMType.ofClassName(frame, "java/lang/invoke/MemberName"));

                        int access = m.getMethodNode().access;
                        flags = calcMethodFlags(access, m.getMethodNode().name.equals("<init>"));
                        memberName.setField("flags", new VMInteger(frame, flags));
                        memberName.setField("method", new VMResolvedMethodName(cl, m));

                        return memberName;
                    }
                }
        );
        clazz.injectMethod(
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
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject memberName = (VMObject) args[0];
                        VMArray arr = new VMArray(frame, VMType.ofGenericObject(frame), 2);
                        VMClassObject clazzObj = (VMClassObject) memberName.getField("clazz");
                        int flags = ((VMInteger) memberName.getField("flags")).asNumber().intValue();
                        byte refKind = obtainRefKind(flags);

                        VMReferenceValue method = (VMReferenceValue) memberName.getField("method");
                        if (method instanceof VMResolvedMethodName resolved)
                        {
                            if (refKind == /* REF_invokeVirtual */ 5 || refKind == /* REF_invokeInterface */ 9)
                                arr.set(0, new VMLong(frame, resolved.getMethod().getSlot()));
                            else
                                arr.set(0, new VMLong(frame, -1));  // static の場合は -1
                            arr.set(1, memberName);
                        }
                        else if (isField(flags))
                        {
                            String fieldName = ((VMStringObject) memberName.getField("name")).getString();
                            VMField field = clazzObj.getRepresentingClass().findField(fieldName);
                            arr.set(0, new VMLong(frame, field.getSlot()));
                            arr.set(1, clazzObj);
                        }

                        return arr;
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "staticFieldOffset",
                        "(Ljava/lang/invoke/MemberName;)J",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject memberName = (VMObject) args[0];
                        VMClassObject clazzObj = (VMClassObject) memberName.getField("clazz");
                        String fieldName = ((VMStringObject) memberName.getField("name")).getString();
                        VMField field = clazzObj.getRepresentingClass().findField(fieldName);
                        return new VMLong(frame, field.getFieldID());
                    }
                }
        );
        clazz.injectMethod(
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "staticFieldBase",
                        "(Ljava/lang/invoke/MemberName;)Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    @Override
                    protected VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMObject memberName = (VMObject) args[0];
                        return (VMClassObject) memberName.getField("clazz");
                    }
                }
        );
    }

    private static MethodDescriptor getDescriptorByMethodType(@NotNull VMObject methodType)
    {
        if (!methodType.getObjectType().getReference().isEqualClass("java/lang/invoke/MethodType"))
            return null;

        VMObject returnType = (VMObject) methodType.getField("rtype");
        VMArray paramTypes = (VMArray) methodType.getField("ptypes");

        String returnTypeDesc = ((VMClassObject) returnType).getRepresentingClass().getTypeDescriptor();
        String[] paramTypeDescs = new String[paramTypes.length()];
        for (int i = 0; i < paramTypes.length(); i++)
        {
            VMObject paramType = (VMObject) paramTypes.get(i);
            paramTypeDescs[i] = ((VMClassObject) paramType).getRepresentingClass().getTypeDescriptor();
        }

        String desc = "(" + String.join("", paramTypeDescs) + ")" + returnTypeDesc;
        return MethodDescriptor.parse(desc);
    }

    private static int calcMethodFlags(int modifier, boolean isConstructor)
    {
        boolean isStatic = (EOpcodes.ACC_STATIC & modifier) != 0;
        return calcFlags(
                isConstructor ? 0x00020000 : 0x00010000,
                modifier,
                isConstructor ? /* REF_newInvokeSpecial */ 8 : isStatic ? /* REF_invokeStatic */ 6 : /* REF_invokeVirtual */ 5
        );
    }

    private static int calcFieldFlags(int modifier)
    {
        boolean isStatic = (EOpcodes.ACC_STATIC & modifier) != 0;
        return calcFlags(0x00040000, modifier, isStatic ? /* REF_getStatic */ 2 : /* REF_getField */ 1);
    }

    public static boolean isField(int flags)
    {
        return (flags & 0x00040000) != 0;
    }

    private static int calcFlags(int is, int modifier, int refKind)
    {
        return is | modifier | (refKind << 24);
    }

    private static byte obtainRefKind(int flags)
    {
        return (byte) ((flags >> 24) & 0xFF);
    }
}
