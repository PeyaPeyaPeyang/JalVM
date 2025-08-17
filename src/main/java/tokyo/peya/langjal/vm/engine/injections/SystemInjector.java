package tokyo.peya.langjal.vm.engine.injections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.FieldNode;
import tokyo.peya.langjal.compiler.jvm.EOpcodes;
import tokyo.peya.langjal.vm.VMSystemClassLoader;
import tokyo.peya.langjal.vm.engine.VMClass;
import tokyo.peya.langjal.vm.references.ClassReference;
import tokyo.peya.langjal.vm.values.VMObject;
import tokyo.peya.langjal.vm.values.VMType;
import tokyo.peya.langjal.vm.values.VMValue;

public class SystemInjector implements Injector
{
    public static final ClassReference CLAZZ = ClassReference.of("java/lang/System");

    @Override
    public ClassReference suitableClass()
    {
        return CLAZZ;
    }

    @Override
    public void inject(@NotNull VMSystemClassLoader cl, @NotNull VMClass clazz)
    {
        clazz.injectField(
                cl,
                new InjectedField(
                        clazz, VMType.ofClassName("java/io/PrintStream"),
                        new FieldNode(
                                EOpcodes.ACC_PUBLIC | EOpcodes.ACC_STATIC,
                                "out",
                                "Ljava/io/PrintStream;",
                                null,
                                null
                        )
                )
                {

                    @Override
                    public VMValue get(@NotNull VMClass caller, @Nullable VMObject instance)
                    {
                        return cl.findClass(CLAZZ).createInstance();
                    }

                    @Override
                    public void set(@NotNull VMClass caller, @Nullable VMObject instance, @NotNull VMValue value)
                    {

                    }
                }
        );
    }

}
