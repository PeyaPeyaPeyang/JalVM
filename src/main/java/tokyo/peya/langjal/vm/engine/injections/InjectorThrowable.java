package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.VMFrame;
import tokyo.peya.langjal.vm.panics.PanicCreator;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMArray;
import tokyo.peya.langjal.vm.values.VMInteger;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMValue;

public class InjectorThrowable implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/Throwable");

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
                        EOpcodes.ACC_PUBLIC | EOpcodes.ACC_SYNCHRONIZED | EOpcodes.ACC_NATIVE,
                        "fillInStackTrace",
                        "(I)Ljava/lang/Throwable;",
                        null,
                        null
                )
                )
                {
                    @Override
                    VMValue invoke(@NotNull VMFrame frame, @Nullable VMClass caller,
                                   @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        assert instance != null;
                        int depth = ((VMInteger) args[0]).asNumber().intValue();
                        VMArray array = PanicCreator.collectStackTrace(frame, depth);
                        instance.setField("backtrace", array);
                        instance.setField("depth", new VMInteger(frame, array.length()));
                        return instance;
                    }
                }
        );
    }

}
