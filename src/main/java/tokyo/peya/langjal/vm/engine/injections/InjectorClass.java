package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.AccessAttribute;
import tokyo.peya.langjal.compiler.jvm.AccessLevel;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMArrayClass;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.engine.members.VMField;
import tokyo.peya.langjal.vm.engine.members.VMMethod;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMNull;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMFieldObject;
import tokyo.peya.langjal.vm.values.metaobjects.reflection.VMMethodObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

public class InjectorClass implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/Class");

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
                ))
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
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "desiredAssertionStatus0",
                        "(Ljava/lang/Class;)Z",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return VMBoolean.ofTrue(frame);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "getPrimitiveClass",
                        "(Ljava/lang/String;)Ljava/lang/Class;",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMStringObject string = (VMStringObject) args[0];
                        String str = string.getString();
                        return VMType.getPrimitiveType(frame, str)
                                     .getLinkedClass()
                                     .getClassObject();
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "isPrimitive",
                        "()Z",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null: "Instance must be a VMObject";
                        VMClassObject instanceClass = (VMClassObject) instance;
                        return VMBoolean.of(frame, instanceClass.getTypeOf().isPrimitive());
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "forName0",
                        "(Ljava/lang/String;ZLjava/lang/ClassLoader;Ljava/lang/Class;)Ljava/lang/Class;",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMStringObject classNameObject = (VMStringObject) args[0];
                        boolean initialize = ((VMBoolean) args[1]).asBoolean();

                        ClassReference classReference = ClassReference.of(classNameObject.getString());
                        VMClass loadedClass = cl.findClass(classReference);
                        if (initialize)
                            loadedClass.initialise(frame.getThread());

                        return loadedClass.getClassObject();
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "isInterface",
                        "()Z",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject instanceClass = (VMClassObject) instance;
                        assert instanceClass != null: "Instance must be a VMClassObject";

                        return VMBoolean.of(
                                frame,
                                instanceClass.getRepresentingClass().getAccessAttributes().has(AccessAttribute.INTERFACE)
                        );
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "isArray",
                        "()Z",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject instanceClass = (VMClassObject) instance;
                        assert instanceClass != null: "Instance must be a VMClassObject";
                        VMClass representingClass = instanceClass.getRepresentingClass();
                        return VMBoolean.of(frame, representingClass instanceof VMArrayClass);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "getDeclaredFields0",
                        "(Z)[Ljava/lang/reflect/Field;",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject instanceClass = (VMClassObject) instance;
                        assert instanceClass != null;
                        VMBoolean publicOnly = (VMBoolean) args[0];
                        VMClass representingClass = instanceClass.getRepresentingClass();

                        VMFieldObject[] fieldObjects;
                        if (publicOnly.asBoolean())
                            fieldObjects = representingClass.getFields().stream()
                                                        .filter(f -> f.getAccessLevel() == AccessLevel.PUBLIC)
                                                        .map(VMField::getFieldObject)
                                                        .toArray(VMFieldObject[]::new);
                        else
                            fieldObjects = representingClass.getFields().stream()
                                                        .map(VMField::getFieldObject)
                                                        .toArray(VMFieldObject[]::new);

                        return new VMArray(
                                frame,
                                cl.findClass(ClassReference.of("java/lang/reflect/Field")),
                                fieldObjects
                        );
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "getDeclaredMethods0",
                        "(Z)[Ljava/lang/reflect/Method;",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject instanceClass = (VMClassObject) instance;
                        assert instanceClass != null;
                        VMBoolean publicOnly = (VMBoolean) args[0];
                        VMClass representingClass = instanceClass.getRepresentingClass();

                        VMMethodObject[] methodObjects;
                        if (publicOnly.asBoolean())
                            methodObjects = representingClass.getMethods().stream()
                                                            .filter(m -> m.getAccessLevel() == AccessLevel.PUBLIC)
                                                            .map(VMMethod::getMethodObject)
                                                            .toArray(VMMethodObject[]::new);
                        else
                            methodObjects = representingClass.getMethods().stream()
                                                            .map(VMMethod::getMethodObject)
                                                            .toArray(VMMethodObject[]::new);

                        return new VMArray(
                                frame,
                                cl.findClass(ClassReference.of("java/lang/reflect/Method")),
                                methodObjects
                        );
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "getDeclaredConstructors0",
                        "(Z)[Ljava/lang/reflect/Constructor;",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject instanceClass = (VMClassObject) instance;
                        assert instanceClass != null;
                        VMBoolean publicOnly = (VMBoolean) args[0];
                        VMClass representingClass = instanceClass.getRepresentingClass();

                        VMMethodObject[] methodObjects;
                        if (publicOnly.asBoolean())
                            methodObjects = representingClass.getMethods().stream()
                                                             .filter(m -> m.getAccessLevel() == AccessLevel.PUBLIC)
                                                             .filter(VMMethod::isConstructor)
                                                             .map(VMMethod::getMethodObject)
                                                             .toArray(VMMethodObject[]::new);
                        else
                            methodObjects = representingClass.getMethods().stream()
                                                             .filter(VMMethod::isConstructor)
                                                             .map(VMMethod::getMethodObject)
                                                             .toArray(VMMethodObject[]::new);

                        return new VMArray(
                                frame,
                                cl.findClass(ClassReference.of("java/lang/reflect/Constructor")),
                                methodObjects
                        );
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "getDeclaredClasses0",
                        "()[Ljava/lang/Class;",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject instanceClass = (VMClassObject) instance;
                        assert instanceClass != null;
                        VMClass representingClass = instanceClass.getRepresentingClass();

                        VMClassObject[] classObjects = representingClass.getInnerLinks().stream()
                                                                        .map(VMClass::getClassObject)
                                                                        .toArray(VMClassObject[]::new);

                        return new VMArray(
                                frame,
                                cl.findClass(ClassReference.of("java/lang/Class")),
                                classObjects
                        );
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "getModifiers",
                        "()I",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject obj = (VMClassObject) instance;
                        assert obj != null;
                        int access = obj.getRepresentingClass().getClazz().access;
                        return new VMInteger(frame, access);
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "initClassName",
                        "()Ljava/lang/String;",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject obj = (VMClassObject) instance;
                        assert obj != null;
                        VMType<?> type = obj.getTypeOf();
                        String name;
                        if (type.getComponentType() == null)
                            name = type.getLinkedClass().getReference().getFullQualifiedDotName();
                        else
                            name = type.getTypeDescriptor();

                        VMValue nameObj = VMStringObject.createString(frame, name);
                        obj.setField("name", nameObj);
                        return nameObj;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "isAssignableFrom",
                        "(Ljava/lang/Class;)Z",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject obj = (VMClassObject) instance;
                        assert obj != null;
                        VMClassObject other = (VMClassObject) args[0];

                        return VMBoolean.of(frame, obj.getTypeOf().isAssignableFrom(other.getTypeOf()));
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "getEnclosingMethod0",
                        "()[Ljava/lang/Object;",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject obj = (VMClassObject) instance;
                        assert obj != null;
                        VMClass clazz = obj.getRepresentingClass();
                        ClassNode node = clazz.getClazz();
                        String outerClass = node.outerClass;
                        String outerMethod = node.outerMethod;
                        String outerMethodDesc = node.outerMethodDesc;

                        VMArray result = new VMArray(frame, VMType.ofGenericObject(frame), 3);
                        if (outerClass == null)
                            return new VMNull<>(VMType.ofGenericObject(frame));
                        else
                        {
                            VMClass outerClazz = cl.findClass(ClassReference.of(outerClass));
                            result.set(0, outerClazz.getClassObject());
                        }
                        if (outerMethod != null)
                            result.set(1, VMStringObject.createString(frame, outerMethod));
                        if (outerMethodDesc != null)
                            result.set(2, VMStringObject.createString(frame, outerMethodDesc));

                        return result;
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PRIVATE | EOpcodes.ACC_NATIVE,
                        "getDeclaringClass0",
                        "()Ljava/lang/Class;",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject obj = (VMClassObject) instance;
                        assert obj != null;
                        VMClass clazz = obj.getRepresentingClass();
                        ClassNode node = clazz.getClazz();
                        String outerClass = node.outerClass;

                        if (outerClass == null)
                            return new VMNull<>(VMType.ofGenericObject(frame));
                        else
                        {
                            VMClass outerClazz = cl.findClass(ClassReference.of(outerClass));
                            return outerClazz.getClassObject();
                        }
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "isHidden",
                        "()Z",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        return VMBoolean.ofFalse(frame);  // TODO: Implement hidden classes
                    }
                }
        );
        clazz.injectMethod(
                cl,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_NATIVE,
                        "isInstance",
                        "(Ljava/lang/Object;)Z",
                        null,
                        null
                ))
                {
                    @Override VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMValue obj = args[0];
                        if (obj instanceof VMNull)
                            return VMBoolean.ofFalse(frame);
                        VMObject objInstance = (VMObject) obj;
                        assert instance != null;
                        VMClassObject classObject = (VMClassObject) instance;
                        return VMBoolean.of(
                                frame,
                                classObject.getRepresentingClass().isAssignableFrom(objInstance.getObjectType())
                        );
                    }
                }
        );
    }

}
