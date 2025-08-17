package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.engine.threads.VMThread;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMStringCreator;
import tokyo.peya.langjal.vm.values.VMValue;

public class PrintStreamInjector implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/io/PrintStream");

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader classLoader, @NotNull VMClass clazz)
    {
        clazz.injectMethod(
                classLoader,
                new InjectedMethod(
                        clazz, new MethodNode(
                        EOpcodes.ACC_PUBLIC,
                        "println",
                        "(Ljava/lang/String;)V",
                        null,
                        null
                )
                )
                {
                    @Override
                    @Nullable VMValue invoke(@NotNull VMThread thread, @Nullable VMClass caller,
                                             @Nullable VMObject instance, @NotNull VMValue[] args)
                    {
                        VMValue arg = args[0];
                        PrintStreamInjector.this.println(arg);
                        return null;
                    }
                }
        );
    }

    private void println(@NotNull VMValue string)
    {
        if (!(string instanceof VMObject vmObject))
            throw new IllegalArgumentException("Expected a VMObject for println, got: " + string);
        String str = VMStringCreator.getString(vmObject);
        System.out.println(str);
    }
}
