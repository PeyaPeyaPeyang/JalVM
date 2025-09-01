package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.threading.VMThread;
import tokyo.peya.langjal.vm.panics.VMPanic;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;
import tokyo.peya.langjal.vm.values.metaobjects.VMClassObject;

public class InjectorArray implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/reflect/Array");

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
                        "newArray",
                        "(Ljava/lang/Class;I)Ljava/lang/Object;",
                        null,
                        null
                )
                )
                {
                    @Override
                    VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                            @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMClassObject obj = (VMClassObject) args[0];
                        int length = ((VMInteger) args[1]).asNumber().intValue();
                        if (length < 0)
                            throw new VMPanic("NegativeArraySizeException");

                        return new VMArray(thread, obj.getRepresentingClass(), length);
                    }
                }
        );
    }

}
