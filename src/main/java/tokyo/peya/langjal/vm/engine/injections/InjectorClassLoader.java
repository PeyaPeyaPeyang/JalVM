package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMBoolean;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMReferenceValue;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;
import tokyo.peya.langjal.vm.values.metaobjects.VMStringObject;

public class InjectorClassLoader implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/ClassLoader");

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
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_STATIC | EOpcodes.ACC_NATIVE,
                        "defineClass0",
                        "(Ljava/lang/ClassLoader;Ljava/lang/Class;Ljava/lang/String;[BIILjava/security/ProtectionDomain;ZILjava/lang/Object;)Ljava/lang/Class;",
                        null,
                        null
                ))
                {
                    @Override
                    @Nullable VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMReferenceValue loader = (VMReferenceValue) args[0];
                        VMClassObject lookup = (VMClassObject) args[1];
                        String name = ((VMStringObject) args[2]).getString();
                        VMArray array = (VMArray) args[3];
                        int offset = ((VMInteger) args[4]).asNumber().intValue();
                        int length = ((VMInteger) args[5]).asNumber().intValue();
                        VMReferenceValue protectionDomain = (VMReferenceValue) args[6];
                        boolean initialise = ((VMBoolean) args[7]).asBoolean();
                        int flags = ((VMInteger) args[8]).asNumber().intValue();
                        VMReferenceValue classData = (VMReferenceValue) args[9];

                        byte[] data = new byte[length];
                        for (int i = 0; i < length; i++)
                            data[i] = ((VMInteger) array.get(i + offset)).asNumber().byteValue();

                        VMClass clazz = frame.getClassLoader().defineClass(data);
                        if (initialise)
                            clazz.initialise(frame.getThread());
                        clazz.setClassData(classData);

                        return clazz.getClassObject();
                    }
                }
        );
    }

}
